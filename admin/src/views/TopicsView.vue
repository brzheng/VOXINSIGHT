<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const topics = ref([])
const error = ref('')

async function load() {
  topics.value = await api('/api/admin/topics')
}

onMounted(async () => {
  try {
    await load()
  } catch (err) {
    error.value = err.message
  }
})

async function remove(topic) {
  if (!window.confirm(`删除「${topic.title}」？已有训练记录的题目不能删除。`)) return
  error.value = ''
  try {
    await api(`/api/admin/topics/${topic.id}`, { method: 'DELETE' })
    topics.value = topics.value.filter((item) => item.id !== topic.id)
  } catch (err) {
    error.value = err.message
  }
}
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">题目</p>
        <h1>题库和内容要点</h1>
      </div>
      <button class="btn primary" type="button" @click="router.push('/topics/new')">新建题目</button>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <article class="card">
      <table class="table">
        <thead>
          <tr><th>标题</th><th>分类</th><th>难度</th><th>时长</th><th>要点</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="topic in topics" :key="topic.id">
            <td>
              {{ topic.title }}
              <span v-if="topic.recommended" class="tag">首页推荐</span>
            </td>
            <td>{{ topic.categoryName }}</td>
            <td>{{ topic.difficulty }}</td>
            <td>{{ topic.durationMin }}–{{ topic.durationMax }} 分钟</td>
            <td>{{ topic.checkpointCount }}</td>
            <td>
              <button class="btn" type="button" @click="router.push(`/topics/${topic.id}`)">编辑</button>
              <button class="btn danger" type="button" @click="remove(topic)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!topics.length" class="empty">还没有题目。</p>
    </article>
  </section>
</template>
