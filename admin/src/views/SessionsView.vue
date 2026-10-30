<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { api, token } from '../api'

const sessions = ref([])
const status = ref('')
const detail = ref(null)
const audioUrl = ref('')
const error = ref('')
const names = {
  pending: '待开始',
  recording: '录制中',
  paused: '已暂停',
  analyzing: '分析中',
  completed: '已完成',
  failed: '分析失败',
}

function clock(seconds) {
  const minute = Math.floor((seconds || 0) / 60)
  const remain = (seconds || 0) % 60
  return minute ? `${minute}分${String(remain).padStart(2, '0')}秒` : `${remain}秒`
}

async function load() {
  const query = status.value ? `?status=${encodeURIComponent(status.value)}` : ''
  sessions.value = await api(`/api/admin/sessions${query}`)
}

onMounted(async () => {
  try {
    await load()
  } catch (err) {
    error.value = err.message
  }
})

onUnmounted(() => {
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
})

async function open(item) {
  error.value = ''
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
  audioUrl.value = ''
  try {
    detail.value = await api(`/api/admin/sessions/${item.id}`)
    if (detail.value.hasAudio) {
      const response = await fetch(`/api/admin/sessions/${item.id}/audio`, {
        headers: { Authorization: `Bearer ${token()}` },
      })
      if (response.ok) audioUrl.value = URL.createObjectURL(await response.blob())
    }
  } catch (err) {
    error.value = err.message
  }
}

async function remove(item) {
  if (!window.confirm('删除这条训练记录后，对应的录音和报告也会删除。')) return
  error.value = ''
  try {
    await api(`/api/admin/sessions/${item.id}`, { method: 'DELETE' })
    sessions.value = sessions.value.filter((row) => row.id !== item.id)
    if (detail.value?.id === item.id) detail.value = null
  } catch (err) {
    error.value = err.message
  }
}
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">训练记录</p>
        <h1>全部学员的练习</h1>
      </div>
      <form class="btn-row" @submit.prevent="load">
        <select v-model="status" style="border:1px solid var(--line);border-radius:10px;padding:8px 12px">
          <option value="">全部状态</option>
          <option v-for="(label, key) in names" :key="key" :value="key">{{ label }}</option>
        </select>
        <button class="btn" type="submit">筛选</button>
      </form>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <article class="card">
      <table class="table">
        <thead>
          <tr><th>学员</th><th>题目</th><th>状态</th><th>时间</th><th>时长</th><th>分数</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="item in sessions" :key="item.id" class="clickable" @click="open(item)">
            <td>{{ item.nickname }}<p class="muted">{{ item.loginName }}</p></td>
            <td>{{ item.title }}</td>
            <td><span class="tag" :class="item.status === 'failed' ? 'danger' : item.status === 'completed' ? 'ok' : 'warn'">{{ names[item.status] || item.status }}</span></td>
            <td>{{ item.endedAt || item.startedAt || '—' }}</td>
            <td>{{ clock(item.durationSeconds) }}</td>
            <td>{{ item.score ?? '—' }}</td>
            <td><button class="btn danger" type="button" @click.stop="remove(item)">删除</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!sessions.length" class="empty">没有训练记录。</p>
    </article>
    <article v-if="detail" class="card" style="margin-top:16px">
      <h2>{{ detail.nickname }} · {{ detail.title }}</h2>
      <p class="muted">{{ names[detail.status] || detail.status }} · {{ clock(detail.durationSeconds) }}<template v-if="detail.resultLabel"> · {{ detail.resultLabel }}</template></p>
      <p v-if="detail.failReason" class="error">{{ detail.failReason }}</p>
      <div v-if="detail.score != null" class="metrics">
        <div class="metric"><span>总分</span><b>{{ detail.score }}</b></div>
        <div class="metric"><span>语速</span><b>{{ detail.speechRate }}</b></div>
        <div class="metric"><span>停顿</span><b>{{ detail.pauseCount }}</b></div>
        <div class="metric"><span>内容分</span><b>{{ detail.contentScore ?? '—' }}</b></div>
      </div>
      <p v-if="detail.transcript">{{ detail.transcript }}</p>
      <p v-if="detail.contentSummary" class="muted">{{ detail.contentSummary }}</p>
      <audio v-if="audioUrl" :src="audioUrl" controls style="width:100%;margin-top:12px"></audio>
    </article>
  </section>
</template>
