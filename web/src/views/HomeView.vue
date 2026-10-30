<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, avatarObjectUrl } from '../api'

const router = useRouter()
const home = ref(null)
const error = ref('')
const avatarUrl = ref('')

const percent = computed(() => {
  if (!home.value?.weeklyTarget) return 0
  return Math.min(100, Math.round((home.value.weeklyDone / home.value.weeklyTarget) * 100))
})

const delta = computed(() => {
  if (!home.value?.latest || home.value.previousScore == null) return ''
  const value = home.value.latest.score - home.value.previousScore
  if (value === 0) return '与上一次相同'
  return value > 0 ? `较上一次 +${value}` : `较上一次 ${value}`
})

onMounted(async () => {
  try {
    home.value = await api('/api/home')
    if (home.value?.user?.hasAvatar) avatarUrl.value = await avatarObjectUrl()
  } catch (err) {
    error.value = err.message
  }
})

onUnmounted(() => {
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value)
})

async function dismiss(id) {
  await api(`/api/notices/${id}/read`, { method: 'POST' })
  home.value.notices = home.value.notices.filter((item) => item.id !== id)
}
</script>

<template>
  <section>
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="home">
      <header class="page-head">
        <div>
          <p class="eyebrow">{{ home.brand.en }}</p>
          <h1>{{ home.user.nickname }}，今天也来练习表达</h1>
          <p class="lead">{{ home.brand.slogan }}</p>
        </div>
        <button class="btn" type="button" @click="router.push('/profile')">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" style="width:22px;height:22px;border-radius:50%;vertical-align:middle;margin-right:6px" />
          {{ home.user.nickname }}
        </button>
      </header>
      <div class="grid two">
        <div class="grid">
          <article class="card">
            <span class="muted">本周进度</span>
            <strong class="score">{{ home.weeklyDone }}<small style="font-size:18px;color:var(--muted)"> / {{ home.weeklyTarget }}</small></strong>
            <div class="bar"><span :style="{ width: `${percent}%` }"></span></div>
          </article>
          <article class="card">
            <span class="muted">最近一次成绩</span>
            <template v-if="home.latest">
              <strong class="score">{{ home.latest.score }}</strong>
              <p>{{ home.latest.title }}</p>
              <p class="muted">{{ home.latest.date }} · {{ home.latest.duration }} <template v-if="delta">· {{ delta }}</template></p>
              <button class="btn primary" type="button" @click="router.push(`/report/${home.latest.reportId}`)">查看报告</button>
            </template>
            <p v-else class="empty">还没有已完成的训练。</p>
          </article>
        </div>
        <div class="grid">
          <article v-if="home.recommended" class="card">
            <span class="tag">推荐题</span>
            <h2>{{ home.recommended.title }}</h2>
            <p class="muted">{{ home.recommended.category }} · {{ home.recommended.difficulty }} · {{ home.recommended.durationMin }}–{{ home.recommended.durationMax }} 分钟</p>
            <button class="btn primary" type="button" @click="router.push(`/practice/${home.recommended.id}`)">开始训练</button>
          </article>
          <article class="card">
            <h2>通知</h2>
            <p v-if="!home.notices.length" class="muted">暂无未读通知。</p>
            <div v-for="item in home.notices" :key="item.id" class="btn-row" style="justify-content:space-between;margin-top:10px">
              <span>{{ item.content }}</span>
              <button class="btn" type="button" @click="dismiss(item.id)">知道了</button>
            </div>
          </article>
        </div>
      </div>
    </template>
  </section>
</template>
