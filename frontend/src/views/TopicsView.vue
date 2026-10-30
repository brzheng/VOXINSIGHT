<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PhoneFrame from '@/components/PhoneFrame.vue'
import { useAppStore } from '@/stores/app'
import { durationLabel } from '@/utils/topic'

const router = useRouter()
const app = useAppStore()
const keyword = ref('')
const category = ref('全部')
const error = ref('')

const visibleTopics = computed(() => {
  const text = keyword.value.trim()
  return app.topics.filter((item) => {
    const matchCategory = category.value === '全部' || item.category === category.value
    return matchCategory && item.title.includes(text)
  })
})

function openTopic(id) {
  router.push(`/practice/${id}`)
}

onMounted(async () => {
  try {
    await app.loadTopics()
  } catch (err) {
    error.value = err.message
  }
})
</script>

<template>
  <PhoneFrame label="训练题库">
    <h1 class="screen-title">训练题库</h1>
    <label class="search">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="11" cy="11" r="6.5" />
        <path d="m16 16 3.5 3.5" />
      </svg>
      <input v-model="keyword" type="search" placeholder="搜索训练题目" />
    </label>

    <div class="chips" role="tablist" aria-label="题目分类">
      <button
        v-for="item in app.categories"
        :key="item"
        type="button"
        role="tab"
        :aria-selected="category === item"
        :class="{ active: category === item }"
        @click="category = item"
      >
        {{ item }}
      </button>
    </div>

    <p v-if="error" class="empty">{{ error }}</p>

    <p v-if="visibleTopics.length === 0" class="empty">没有找到相关题目</p>

    <article v-for="topic in visibleTopics" :key="topic.id" class="card item" @click="openTopic(topic.id)">
      <div class="head">
        <h2>{{ topic.title }}</h2>
        <button
          type="button"
          class="heart"
          :class="{ on: app.isFavorite(topic.id) }"
          :aria-label="app.isFavorite(topic.id) ? '取消收藏' : '收藏'"
          :aria-pressed="app.isFavorite(topic.id)"
          @click.stop="app.toggleFavorite(topic.id)"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 19.2 5.6 13a3.8 3.8 0 0 1 5.4-5.4L12 8.8l1-1.2a3.8 3.8 0 0 1 5.4 5.4L12 19.2Z"
            />
          </svg>
        </button>
      </div>
      <ul>
        <li>{{ topic.category }}</li>
        <li>{{ topic.difficulty }}</li>
        <li>{{ durationLabel(topic) }}</li>
      </ul>
      <button type="button" class="go" @click.stop="openTopic(topic.id)">开始练习</button>
    </article>
  </PhoneFrame>
</template>

<style scoped>
.search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  margin-top: 16px;
  padding: 0 12px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(32, 38, 58, 0.04);
}

.search svg {
  width: 18px;
  height: 18px;
  flex: none;
  fill: none;
  stroke: var(--muted);
  stroke-width: 1.8;
  stroke-linecap: round;
}

.search input {
  width: 100%;
  border: 0;
  outline: none;
  color: var(--text);
  background: transparent;
  font-size: 14px;
}

.chips {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  overflow: auto;
}

.chips button {
  flex: none;
  height: 32px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  color: var(--muted);
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}

.chips button.active {
  color: #fff;
  background: var(--brand);
  font-weight: 700;
}

.item {
  margin-top: 12px;
  padding: 16px;
  cursor: pointer;
}

.head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

h2 {
  flex: 1;
  margin: 0;
  font-size: 16px;
  line-height: 1.5;
}

.heart {
  width: 32px;
  height: 32px;
  flex: none;
  border: 0;
  border-radius: 10px;
  color: #b7c0d4;
  background: #f5f7fc;
  cursor: pointer;
}

.heart svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linejoin: round;
}

.heart.on {
  color: var(--danger);
  background: #fdecef;
}

.heart.on svg {
  fill: currentColor;
}

ul {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

li {
  padding: 4px 8px;
  border-radius: 999px;
  color: var(--brand);
  background: #eef0fa;
  font-size: 12px;
}

.go {
  margin-top: 12px;
  padding: 0;
  border: 0;
  color: var(--brand);
  background: transparent;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.empty {
  margin: 28px 0;
  color: var(--muted);
  font-size: 14px;
  text-align: center;
}
</style>
