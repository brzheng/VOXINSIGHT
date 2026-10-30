<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { api, token } from '../api'

const route = useRoute()
const router = useRouter()
const topic = ref(null)
const ready = ref(false)
const status = ref('idle')
const resumeTo = ref('paused')
const seconds = ref(0)
const error = ref('')
const failReason = ref('')
const sessionId = ref(null)
const bars = ref([10, 10, 10, 10, 10, 10, 10, 10, 10])
const maxSeconds = 1800
let timer
let draftTimer
let stream = null
let recorder = null
let chunks = []
let mimeType = ''
let recordedBlob = null
let prefixBlob = null
let audioContext = null
let analyser = null
let meterSource = null
let meterFrame = 0

const clock = computed(() => {
  const minute = String(Math.floor(seconds.value / 60)).padStart(2, '0')
  const second = String(seconds.value % 60).padStart(2, '0')
  return `${minute}:${second}`
})

const statusText = computed(() => {
  if (status.value === 'analyzing') return '分析中'
  if (status.value === 'failed') return '分析失败'
  if (status.value === 'recording' || (status.value === 'confirm' && resumeTo.value === 'recording')) return '录制中'
  if (status.value === 'paused' || status.value === 'confirm') return '已暂停'
  return '待开始'
})

const recoveryText = computed(() => {
  if (status.value === 'failed') return failReason.value || '分析没有完成，可以重试，或放弃后重新录音。'
  if (recordedBlob && status.value === 'paused' && !recorder) return '上次录音已保存。可以接着录，提交分析，或放弃后重录。'
  if (sessionId.value && status.value === 'paused' && !recordedBlob && !recorder) {
    return '有一次暂停的训练，当时没有保存录音。可以继续录，或放弃。'
  }
  return ''
})

function pickMime() {
  if (typeof MediaRecorder === 'undefined') return ''
  const types = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg']
  return types.find((type) => MediaRecorder.isTypeSupported(type)) || ''
}

function micMessage(err) {
  if (err?.name === 'NotAllowedError' || err?.name === 'PermissionDeniedError') return '需要允许使用麦克风才能录音。'
  if (err?.name === 'NotFoundError') return '没有找到可用的麦克风。'
  if (err?.name === 'NotReadableError') return '麦克风正被其他程序占用。'
  return err?.message || '无法开始录音。'
}

function releaseMic() {
  stopMeter(true)
  stream?.getTracks().forEach((track) => track.stop())
  stream = null
  recorder = null
}

function prepareMeter() {
  const Context = window.AudioContext || window.webkitAudioContext
  if (!Context) return
  if (!audioContext) audioContext = new Context()
  audioContext.resume().catch(() => {})
}

function connectMeter() {
  if (!audioContext || !stream) return
  meterSource?.disconnect()
  analyser = audioContext.createAnalyser()
  analyser.fftSize = 256
  meterSource = audioContext.createMediaStreamSource(stream)
  meterSource.connect(analyser)
  const bins = new Uint8Array(analyser.frequencyBinCount)
  const draw = () => {
    if (!analyser) return
    analyser.getByteFrequencyData(bins)
    const slice = Math.floor(bins.length / bars.value.length)
    const next = []
    for (let index = 0; index < bars.value.length; index += 1) {
      let sum = 0
      for (let offset = 0; offset < slice; offset += 1) sum += bins[index * slice + offset]
      next.push(8 + Math.round((sum / slice / 255) * 48))
    }
    bars.value = next
    meterFrame = requestAnimationFrame(draw)
  }
  cancelAnimationFrame(meterFrame)
  meterFrame = requestAnimationFrame(draw)
}

function stopMeter(close) {
  cancelAnimationFrame(meterFrame)
  bars.value = [10, 10, 10, 10, 10, 10, 10, 10, 10]
  if (!close) return
  meterSource?.disconnect()
  meterSource = null
  analyser = null
  audioContext?.close()
  audioContext = null
}

async function ensureRecorder() {
  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
    throw new Error('当前浏览器不能使用麦克风。请用 localhost 打开页面。')
  }
  if (!stream) stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  if (!recorder || recorder.state === 'inactive') {
    mimeType = pickMime()
    try {
      recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined)
    } catch {
      releaseMic()
      throw new Error('当前浏览器不能录制音频。')
    }
    recorder.addEventListener('dataavailable', (event) => {
      if (event.data.size > 0) chunks.push(event.data)
    })
    recorder.start(1000)
  } else if (recorder.state === 'paused') {
    recorder.resume()
  }
}

function pauseRecorder() {
  if (recorder?.state === 'recording') recorder.pause()
}

function stopRecorder() {
  return new Promise((resolve) => {
    if (!recorder || recorder.state === 'inactive') {
      resolve()
      return
    }
    recorder.addEventListener('stop', () => resolve(), { once: true })
    recorder.stop()
  })
}

async function finishBlob() {
  await stopRecorder()
  const type = (mimeType || 'audio/webm').split(';')[0]
  const blob = new Blob(chunks, { type })
  chunks = []
  releaseMic()
  return blob
}

function extensionFor(type) {
  if (type.includes('mp4')) return 'm4a'
  if (type.includes('ogg')) return 'ogg'
  if (type.includes('wav')) return 'wav'
  if (type.includes('mpeg')) return 'mp3'
  return 'webm'
}

function tick() {
  clearInterval(timer)
  timer = setInterval(() => {
    seconds.value += 1
    if (seconds.value >= maxSeconds) {
      clearInterval(timer)
      pauseRecorder()
      stopMeter(false)
      status.value = 'paused'
      error.value = '单次录音最长 30 分钟。'
    }
  }, 1000)
}

function currentType() {
  return (mimeType || recordedBlob?.type || 'audio/webm').split(';')[0]
}

async function saveDraft() {
  if (!sessionId.value || chunks.length === 0) return
  const blob = new Blob(chunks, { type: currentType() })
  if (!blob.size) return
  const form = new FormData()
  form.append('durationSeconds', String(Math.min(Math.max(seconds.value, 0), maxSeconds)))
  if (prefixBlob) form.append('prefix', prefixBlob, `prefix.${extensionFor(prefixBlob.type || 'audio/webm')}`)
  form.append('audio', blob, `practice.${extensionFor(blob.type)}`)
  await api(`/api/sessions/${sessionId.value}/audio`, { method: 'POST', form })
}

async function loadSavedAudio(id) {
  const response = await fetch(`/api/sessions/${id}/audio`, {
    headers: { Authorization: `Bearer ${token()}` },
  })
  if (!response.ok) return
  recordedBlob = await response.blob()
  mimeType = recordedBlob.type || mimeType
}

async function recover(state) {
  sessionId.value = state.sessionId
  seconds.value = state.durationSeconds || 0
  failReason.value = state.failReason || ''
  if (state.hasAudio) await loadSavedAudio(state.sessionId)
  status.value = state.status === 'failed' || state.status === 'analyzing' ? 'failed' : 'paused'
}

async function discard() {
  clearInterval(timer)
  clearInterval(draftTimer)
  if (sessionId.value) await api(`/api/sessions/${sessionId.value}`, { method: 'DELETE' })
  sessionId.value = null
  recordedBlob = null
  prefixBlob = null
  chunks = []
  seconds.value = 0
  failReason.value = ''
  status.value = 'idle'
  releaseMic()
}

async function start() {
  error.value = ''
  if (status.value === 'failed') await discard()
  prepareMeter()
  const topicId = Number(route.params.topicId)
  try {
    if (!sessionId.value) {
      const created = await api('/api/sessions/start', { method: 'POST', body: { topicId } })
      if (created.status !== 'recording' || created.hasAudio) {
        await recover(created)
        stopMeter(true)
        return
      }
      sessionId.value = created.sessionId
    } else if (status.value === 'paused') {
      await api(`/api/sessions/${sessionId.value}/resume`, { method: 'POST' })
    }
    const continuing = recorder?.state === 'paused'
    if (!continuing && recordedBlob) {
      prefixBlob = recordedBlob
      recordedBlob = null
      chunks = []
    } else if (!continuing) {
      prefixBlob = null
      chunks = []
      recordedBlob = null
    }
    await ensureRecorder()
    connectMeter()
  } catch (err) {
    error.value = micMessage(err)
    if (!recordedBlob && prefixBlob && chunks.length === 0) {
      recordedBlob = prefixBlob
      prefixBlob = null
    }
    stopMeter(true)
    return
  }
  status.value = 'recording'
  tick()
  clearInterval(draftTimer)
  draftTimer = setInterval(() => {
    saveDraft().catch((err) => {
      error.value = err.message
    })
  }, 15000)
}

async function pause() {
  clearInterval(timer)
  clearInterval(draftTimer)
  if (recorder?.state === 'recording' && typeof recorder.requestData === 'function') recorder.requestData()
  pauseRecorder()
  stopMeter(false)
  status.value = 'paused'
  try {
    if (sessionId.value) {
      await api(`/api/sessions/${sessionId.value}/pause`, {
        method: 'POST',
        body: { durationSeconds: Math.min(seconds.value, maxSeconds) },
      })
      await saveDraft()
    }
  } catch (err) {
    error.value = err.message
  }
}

async function askFinish() {
  const wasRecording = status.value === 'recording'
  if (wasRecording) await pause()
  else clearInterval(timer)
  resumeTo.value = wasRecording ? 'recording' : 'paused'
  status.value = 'confirm'
}

function continueTraining() {
  error.value = ''
  if (resumeTo.value === 'recording') start()
  else status.value = 'paused'
}

async function generateReport() {
  clearInterval(draftTimer)
  status.value = 'analyzing'
  error.value = ''
  try {
    if (!recordedBlob) recordedBlob = await finishBlob()
    if (!recordedBlob.size) throw new Error('没有录到声音，请检查麦克风后重试。')
    const form = new FormData()
    form.append('durationSeconds', String(Math.min(Math.max(seconds.value, 1), maxSeconds)))
    if (prefixBlob) form.append('prefix', prefixBlob, `prefix.${extensionFor(prefixBlob.type || 'audio/webm')}`)
    form.append('audio', recordedBlob, `practice.${extensionFor(recordedBlob.type)}`)
    const created = await api(`/api/sessions/${sessionId.value}/finish`, { method: 'POST', form })
    sessionId.value = null
    router.push({ name: 'report', params: { reportId: created.reportId } })
  } catch (err) {
    status.value = 'failed'
    failReason.value = err.message
    error.value = err.message
  }
}

async function retry() {
  status.value = 'analyzing'
  error.value = ''
  try {
    const created = await api(`/api/sessions/${sessionId.value}/retry`, { method: 'POST' })
    sessionId.value = null
    router.push({ name: 'report', params: { reportId: created.reportId } })
  } catch (err) {
    status.value = 'failed'
    failReason.value = err.message
    error.value = err.message
  }
}

async function recordAgain() {
  await discard()
  await start()
}

onMounted(async () => {
  try {
    const data = await api('/api/topics')
    topic.value = data.topics.find((item) => String(item.id) === String(route.params.topicId)) || null
    ready.value = true
    if (!topic.value) return
    const active = await api(`/api/sessions/active?topicId=${route.params.topicId}`)
    if (!active) return
    if (active.status === 'recording') {
      await api(`/api/sessions/${active.sessionId}/pause`, {
        method: 'POST',
        body: { durationSeconds: active.durationSeconds || 0 },
      })
      active.status = 'paused'
    }
    await recover(active)
  } catch (err) {
    ready.value = true
    error.value = err.message
  }
})

onBeforeRouteLeave(async () => {
  clearInterval(timer)
  clearInterval(draftTimer)
  if (status.value === 'recording') pauseRecorder()
  try {
    await saveDraft()
  } catch {
    // 离开页面时草稿保存失败，下次进入仍可看到服务端已有的录音。
  }
})

onUnmounted(() => {
  clearInterval(timer)
  clearInterval(draftTimer)
  releaseMic()
})
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">表达训练</p>
        <h1>{{ topic?.title || '录音' }}</h1>
        <p v-if="topic" class="lead">{{ topic.guide }}</p>
      </div>
      <button class="btn" type="button" @click="router.push('/topics')">返回题库</button>
    </header>
    <p v-if="error && status !== 'confirm' && status !== 'analyzing'" class="error">{{ error }}</p>
    <p v-if="!ready" class="muted">正在打开题目。</p>
    <p v-else-if="!topic" class="empty">没有找到这道题目。</p>
    <article v-else class="card">
      <p class="eyebrow">{{ statusText }}</p>
      <p v-if="recoveryText" class="muted">
        {{ recoveryText }}
        <button v-if="recordedBlob && !recorder && status !== 'recording'" class="btn" type="button" @click="recordAgain">放弃重录</button>
        <button v-else-if="sessionId && status !== 'recording'" class="btn" type="button" @click="discard">放弃这次</button>
      </p>
      <div class="wave" :class="{ live: status === 'recording' }" aria-hidden="true">
        <i v-for="(height, index) in bars" :key="index" :style="{ height: `${height}px` }"></i>
      </div>
      <div class="clock">{{ clock }}</div>
      <div class="btn-row">
        <button v-if="status === 'idle'" class="btn primary" type="button" @click="start">开始录音</button>
        <button v-else-if="status === 'recording'" class="btn" type="button" @click="pause">暂停</button>
        <button v-else-if="status === 'failed'" class="btn" type="button" @click="recordAgain">重新录音</button>
        <button v-else class="btn" type="button" @click="start">继续录音</button>
        <button v-if="status === 'failed' && recordedBlob" class="btn primary" type="button" @click="retry">重试分析</button>
        <button v-else-if="status !== 'failed'" class="btn primary" type="button" :disabled="status === 'idle'" @click="askFinish">结束训练</button>
      </div>
    </article>

    <div v-if="status === 'confirm'" class="dialog-mask">
      <div class="card dialog" role="dialog">
        <h2>确定结束本次训练吗？</h2>
        <p>结束后会保存录音，分别给出表达分数和内容分数。</p>
        <p v-if="error" class="error">{{ error }}</p>
        <div class="btn-row">
          <button class="btn" type="button" @click="continueTraining">继续训练</button>
          <button class="btn primary" type="button" @click="generateReport">结束并生成报告</button>
        </div>
      </div>
    </div>
    <div v-else-if="status === 'analyzing'" class="dialog-mask">
      <div class="card dialog">
        <h2>正在识别语音并生成评分</h2>
        <p class="muted">表达分看语速和停顿，内容分看是否覆盖题目要点。</p>
      </div>
    </div>
  </section>
</template>
