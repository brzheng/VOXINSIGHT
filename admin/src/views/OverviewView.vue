<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const overview = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    overview.value = await api('/api/admin/overview')
  } catch (err) {
    error.value = err.message
  }
})
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">概览</p>
        <h1>今天的声析</h1>
      </div>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="overview" class="grid stats">
      <RouterLink class="card stat" to="/users"><span>用户</span><strong>{{ overview.userCount }}</strong></RouterLink>
      <RouterLink class="card stat" to="/topics"><span>题目</span><strong>{{ overview.topicCount }}</strong></RouterLink>
      <RouterLink class="card stat" to="/sessions"><span>训练记录</span><strong>{{ overview.sessionCount }}</strong></RouterLink>
      <RouterLink class="card stat" to="/feedbacks"><span>待处理反馈</span><strong>{{ overview.pendingFeedbackCount }}</strong></RouterLink>
    </div>
  </section>
</template>
