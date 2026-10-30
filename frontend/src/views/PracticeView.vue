<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import PhoneFrame from '@/components/PhoneFrame.vue'
import { api, token } from '@/api/http'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const topic = computed(() => app.findTopic(route.params.topicId))
const status = ref('idle')
const resumeTo = ref('paused')
const seconds = ref(0)
const error = ref('')
const failReason = ref('')
const sessionId = ref(null)
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
const bars = ref([10, 10, 10, 10, 10, 10, 10, 10, 10])

const clock = computed(() => {
  const minute = String(Math.floor(seconds.value / 60)).padStart(2, '0')
  const second = String(seconds.value % 60).padStart(2, '0')
  return `${minute}:${second}`
})

const statusText = computed(() => {
  if (status.value === 'analyzing') return '分析中'
  if (status.value === 'failed') return '分析失败'
  if (status.value === 'recording' || (status.value === 'confirm' && resumeTo.value === 'recording')) {
    return '录制中'
  }
  if (status.value === 'paused' || status.value === 'confirm') return '已暂停'
  return '待开始'
})

const recoveryText = computed(() => {
  if (status.value === 'failed') {
    return failReason.value || '分析没有完成，可以重试，或放弃后重新录音。'
  }
  if (recordedBlob && status.value === 'paused' && !recorder) {
    return '上次录音已保存。可以接着录，提交分析，或放弃后重录。'
  }
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
  if (err?.name === 'NotAllowedError' || err?.name === 'PermissionDeniedError') {
    return '需要允许使用麦克风才能录音。'
  }
  if (err?.name === 'NotFoundError') {
    return '没有找到可用的麦克风。'
  }
  if (err?.name === 'NotReadableError') {
    return '麦克风正被其他程序占用。'
  }
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
      next.push(8 + Math.round((sum / slice / 255) * 34))
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
  if (!stream) {
    stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  }
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
  if (sessionId.value) {
    await api(`/api/sessions/${sessionId.value}`, { method: 'DELETE' })
  }
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
  if (recorder?.state === 'recording' && typeof recorder.requestData === 'function') {
    recorder.requestData()
  }
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
    if (!recordedBlob) {
      recordedBlob = await finishBlob()
    }
    if (!recordedBlob.size) {
      throw new Error('没有录到声音，请检查麦克风后重试。')
    }
    const form = new FormData()
    form.append('durationSeconds', String(Math.min(Math.max(seconds.value, 1), maxSeconds)))
    if (prefixBlob) {
      form.append('prefix', prefixBlob, `prefix.${extensionFor(prefixBlob.type || 'audio/webm')}`)
    }
    form.append('audio', recordedBlob, `practice.${extensionFor(recordedBlob.type)}`)
    const created = await api(`/api/sessions/${sessionId.value}/finish`, { method: 'POST', form })
    clearInterval(draftTimer)
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
    await app.loadTopics()
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
    // 离开页面时草稿保存失败，下次进入仍可看到服务端已有的录音
  }
})

onUnmounted(() => {
  clearInterval(timer)
  clearInterval(draftTimer)
  releaseMic()
})
</script>

<template>
  <PhoneFrame :tabs="false" label="表达训练">
    <template #bar>
      <PageHeader title="表达训练" back-to="/topics" />
    </template>

    <template v-if="topic">
      <article class="card prompt">
        <p>当前题目</p>
        <h2>{{ topic.title }}</h2>
        <small>{{ topic.guide }}</small>
        <p v-if="recoveryText" class="recovery">
          {{ recoveryText }}
          <button v-if="recordedBlob && !recorder && status !== 'recording'" type="button" @click="recordAgain">
            放弃重录
          </button>
          <button v-else-if="sessionId && status !== 'recording'" type="button" @click="discard">放弃这次</button>
        </p>
      </article>

      <div class="stage-record">
        <p class="state">{{ statusText }}</p>
        <div class="wave" :class="{ live: status === 'recording' }" aria-hidden="true">
          <span v-for="(height, index) in bars" :key="index" :style="{ height: `${height}px` }"></span>
        </div>
        <button
          type="button"
          class="record"
          :class="{ live: status === 'recording' }"
          :aria-label="status === 'recording' ? '暂停录音' : status === 'idle' ? '开始录音' : '继续录音'"
          @click="status === 'recording' ? pause() : start()"
        >
          <i v-if="status === 'recording'"></i>
          <b v-else></b>
        </button>
        <strong>{{ clock }}</strong>
        <p v-if="error && status !== 'confirm' && status !== 'analyzing'" class="mic-error">{{ error }}</p>
      </div>
    </template>
    <p v-else class="missing">没有找到这道题目。</p>

    <template v-if="topic" #dock>
      <button v-if="status === 'idle'" type="button" class="btn btn-primary" @click="start">开始录音</button>
      <button v-else-if="status === 'recording'" type="button" class="btn btn-ghost" @click="pause">暂停</button>
      <button v-else-if="status === 'failed'" type="button" class="btn btn-ghost" @click="recordAgain">重新录音</button>
      <button v-else-if="recordedBlob && !recorder" type="button" class="btn btn-ghost" @click="start">
        继续录音
      </button>
      <button v-else type="button" class="btn btn-ghost" @click="start">继续</button>
      <button v-if="status === 'failed' && recordedBlob" type="button" class="btn btn-primary" @click="retry">
        重试分析
      </button>
      <button
        v-else-if="status !== 'failed'"
        type="button"
        class="btn btn-primary"
        :disabled="status === 'idle'"
        @click="askFinish"
      >
        结束训练
      </button>
    </template>

    <template #overlay>
      <div v-if="status === 'confirm'" class="mask">
        <div class="dialog" role="dialog" aria-labelledby="finish-title">
          <h2 id="finish-title">确定结束本次训练吗？</h2>
          <p>结束后会保存录音，分别给出表达分数和内容分数。</p>
          <p v-if="error">{{ error }}</p>
          <div class="dialog-actions">
            <button type="button" class="btn btn-ghost" @click="continueTraining">继续训练</button>
            <button type="button" class="btn btn-primary" @click="generateReport">结束并生成报告</button>
          </div>
        </div>
      </div>
      <div v-else-if="status === 'analyzing'" class="loading">
        <span class="spinner" aria-hidden="true"></span>
        <p>正在识别语音并生成评分</p>
        <small>表达分看语速和停顿，内容分看是否覆盖题目要点</small>
      </div>
    </template>
  </PhoneFrame>
</template>

<style scoped>
.prompt {
  margin-top: 8px;
  padding: 16px;
}

.prompt p,
.prompt small,
.missing {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.prompt h2 {
  margin: 8px 0;
  font-size: 18px;
  line-height: 1.5;
}

.recovery {
  margin-top: 8px;
  line-height: 1.5;
}

.recovery button {
  margin-left: 8px;
  border: 0;
  padding: 0;
  color: var(--brand);
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.stage-record {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 28px;
}

.state {
  margin: 0;
  color: var(--brand);
  font-size: 14px;
  font-weight: 700;
}

.wave {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 48px;
  margin-top: 18px;
}

.wave span {
  width: 4px;
  height: 10px;
  border-radius: 99px;
  background: #c9d0e8;
  transition: height 0.05s linear;
}

.wave.live span {
  background: var(--ai);
}

.record {
  width: 92px;
  height: 92px;
  margin-top: 18px;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: var(--brand);
  box-shadow: 0 12px 28px rgba(57, 73, 171, 0.28);
  cursor: pointer;
}

.record.live {
  background: var(--danger);
}

.record b {
  display: block;
  width: 22px;
  height: 22px;
  margin: 0 auto;
  border-radius: 50%;
  background: #fff;
}

.record i {
  display: block;
  width: 18px;
  height: 18px;
  margin: 0 auto;
  border-radius: 4px;
  background: #fff;
}

.stage-record strong {
  margin-top: 16px;
  font-size: 28px;
  letter-spacing: 0.04em;
}

.mic-error {
  max-width: 280px;
  margin: 12px 0 0;
  color: var(--danger);
  font-size: 13px;
  text-align: center;
}

.missing {
  margin-top: 24px;
  text-align: center;
}

.loading {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg);
}

.loading p {
  margin: 16px 0 0;
  font-size: 16px;
  font-weight: 700;
}

.loading small {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e0e5f4;
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
