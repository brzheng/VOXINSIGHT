<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const feedbacks = ref([])
const status = ref('')
const error = ref('')

async function load() {
  const query = status.value ? `?status=${encodeURIComponent(status.value)}` : ''
  feedbacks.value = await api(`/api/admin/feedbacks${query}`)
}

onMounted(async () => {
  try {
    await load()
  } catch (err) {
    error.value = err.message
  }
})

async function save(item, nextStatus) {
  error.value = ''
  try {
    const saved = await api(`/api/admin/feedbacks/${item.id}`, {
      method: 'PUT',
      body: { status: nextStatus, reply: item.reply },
    })
    Object.assign(item, saved)
    if (status.value && saved.status !== status.value) {
      feedbacks.value = feedbacks.value.filter((row) => row.id !== saved.id)
    }
  } catch (err) {
    error.value = err.message
  }
}
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">反馈</p>
        <h1>用户提交的意见</h1>
      </div>
      <form class="btn-row" @submit.prevent="load">
        <select v-model="status" style="border:1px solid var(--line);border-radius:10px;padding:8px 12px">
          <option value="">全部</option>
          <option>待处理</option>
          <option>已回复</option>
        </select>
        <button class="btn" type="submit">筛选</button>
      </form>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="grid cards">
      <article v-for="item in feedbacks" :key="item.id" class="card">
        <div class="btn-row">
          <span class="tag">{{ item.category }}</span>
          <span class="tag" :class="item.status === '已回复' ? 'ok' : 'warn'">{{ item.status }}</span>
        </div>
        <h2>{{ item.nickname }}</h2>
        <p class="muted">{{ item.loginName }} · {{ item.createdAt }}</p>
        <p>{{ item.content }}</p>
        <label class="field">
          回复
          <textarea v-model="item.reply" maxlength="500" rows="3"></textarea>
        </label>
        <p v-if="item.repliedAt" class="muted">上次回复于 {{ item.repliedAt }}</p>
        <div class="btn-row">
          <button class="btn primary" type="button" @click="save(item, '已回复')">标记已回复</button>
          <button v-if="item.status === '已回复'" class="btn" type="button" @click="save(item, '待处理')">改回待处理</button>
        </div>
      </article>
    </div>
    <p v-if="!feedbacks.length" class="empty">没有反馈。</p>
  </section>
</template>
