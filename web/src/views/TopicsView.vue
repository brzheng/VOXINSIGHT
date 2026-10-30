<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const categories = ref(['全部'])
const topics = ref([])
const category = ref('全部')
const keyword = ref('')
const error = ref('')

const visible = computed(() =>
  topics.value.filter((item) => {
    const categoryOk = category.value === '全部' || item.category === category.value
    const text = keyword.value.trim()
    return categoryOk && (!text || item.title.includes(text))
  }),
)

onMounted(async () => {
  try {
    const data = await api('/api/topics')
    categories.value = data.categories
    topics.value = data.topics
  } catch (err) {
    error.value = err.message
  }
})

async function favorite(topic) {
  error.value = ''
  try {
    const data = await api(`/api/topics/${topic.id}/favorite`, { method: 'POST' })
    topic.favorite = data.favorite
  } catch (err) {
    error.value = err.message
  }
}
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">题库</p>
        <h1>选一道题，开始录音</h1>
      </div>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="filters">
      <button
        v-for="item in categories"
        :key="item"
        class="chip"
        :class="{ on: category === item }"
        type="button"
        @click="category = item"
      >
        {{ item }}
      </button>
      <input v-model="keyword" placeholder="按标题筛选" style="margin-left:auto;border:1px solid var(--line);border-radius:10px;padding:8px 12px" />
    </div>
    <div class="grid cards">
      <article v-for="topic in visible" :key="topic.id" class="card">
        <div class="btn-row">
          <span class="tag">{{ topic.category }}</span>
          <span class="tag">{{ topic.difficulty }}</span>
        </div>
        <h2>{{ topic.title }}</h2>
        <p class="muted">{{ topic.durationMin }}–{{ topic.durationMax }} 分钟 · {{ topic.guide }}</p>
        <div class="btn-row">
          <button class="btn primary" type="button" @click="router.push(`/practice/${topic.id}`)">开始训练</button>
          <button class="btn" type="button" @click="favorite(topic)">{{ topic.favorite ? '已收藏' : '收藏' }}</button>
        </div>
      </article>
    </div>
    <p v-if="!error && !visible.length" class="empty">没有符合条件的题目。</p>
  </section>
</template>
