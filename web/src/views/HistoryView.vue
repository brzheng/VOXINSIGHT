<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const history = ref(null)
const error = ref('')

const names = { recording: '录制中', paused: '已暂停', analyzing: '分析中', failed: '分析失败', pending: '待开始' }

onMounted(async () => {
  try {
    history.value = await api('/api/history')
  } catch (err) {
    error.value = err.message
  }
})

function openRecord(item) {
  if (item.reportId) router.push(`/report/${item.reportId}`)
  else router.push(`/practice/${item.topicId}`)
}

async function clearHistory() {
  if (!window.confirm('清空后，训练记录和报告会永久删除。')) return
  error.value = ''
  try {
    await api('/api/history', { method: 'DELETE' })
    history.value = await api('/api/history')
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
        <h1>已经完成和还没交卷的练习</h1>
      </div>
      <button v-if="history?.keepHistory" class="btn danger" type="button" @click="clearHistory">清空记录</button>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="history">
      <p v-if="!history.keepHistory" class="card">训练历史已隐藏。到个人中心重新打开「保留训练历史」后会恢复。</p>
      <div v-else class="grid stats">
        <article class="card stat"><span>完成次数</span><strong>{{ history.stats.count }}</strong></article>
        <article class="card stat"><span>累计时长</span><strong>{{ history.stats.durationText }}</strong></article>
        <article class="card stat"><span>平均分</span><strong>{{ history.stats.average ?? '—' }}</strong></article>
        <article class="card stat"><span>记录条数</span><strong>{{ history.records.length }}</strong></article>
      </div>
      <article v-if="history.keepHistory" class="card" style="margin-top:16px">
        <table class="table">
          <thead>
            <tr><th>题目</th><th>时间</th><th>时长</th><th>状态</th><th>分数</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="item in history.records" :key="item.sessionId">
              <td>{{ item.title }}</td>
              <td>{{ item.date }}</td>
              <td>{{ item.duration }}</td>
              <td>
                <span v-if="item.status !== 'completed'" class="tag" :class="item.status === 'failed' ? 'danger' : 'warn'">
                  {{ names[item.status] || item.status }}
                </span>
                <span v-else class="tag ok">已完成</span>
                <p v-if="item.failReason" class="muted">{{ item.failReason }}</p>
              </td>
              <td>{{ item.score ?? '—' }}</td>
              <td>
                <button class="btn" type="button" @click="openRecord(item)">{{ item.reportId ? '报告' : '继续' }}</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!history.records.length" class="empty">还没有训练记录。</p>
      </article>
    </template>
  </section>
</template>
