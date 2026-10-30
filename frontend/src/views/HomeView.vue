<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PhoneFrame from '@/components/PhoneFrame.vue'
import { api, avatarObjectUrl } from '@/api/http'
import { durationLabel } from '@/utils/topic'

const router = useRouter()
const home = ref(null)
const error = ref('')
const avatarUrl = ref('')

const progressPercent = computed(() => {
  if (!home.value?.weeklyTarget) return 0
  return Math.min(100, Math.round((home.value.weeklyDone / home.value.weeklyTarget) * 100))
})

const deltaText = computed(() => {
  if (!home.value?.latest || home.value.previousScore == null) return ''
  const delta = home.value.latest.score - home.value.previousScore
  return delta > 0 ? `+${delta}` : String(delta)
})

onMounted(async () => {
  try {
    home.value = await api('/api/home')
    if (home.value?.user?.hasAvatar) {
      avatarUrl.value = await avatarObjectUrl()
    }
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
  <PhoneFrame label="声析首页">
    <template #bar>
      <header class="topbar">
        <div class="brand">
          <span class="mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M4 14.5c1.2-1.6 2.2-2.4 3.2-2.4 1.4 0 1.6 3.2 3 3.2s1.7-6.3 3.2-6.3 1.8 4.2 3.1 4.2 2-.8 3.3-2.2"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <span v-if="home">
            <strong>{{ home.brand.zh }}</strong>
            <small>{{ home.brand.en }}</small>
          </span>
        </div>
        <button
          v-if="home"
          class="avatar"
          type="button"
          :aria-label="home.user.nickname"
          @click="router.push('/profile')"
        >
          {{ home.user.initial }}
          <img v-if="avatarUrl" :src="avatarUrl" alt="" />
        </button>
      </header>
    </template>

    <p v-if="error" class="missing">{{ error }}</p>
    <template v-if="home">
    <p class="welcome">今天也来练习表达吧</p>
    <p class="slogan">{{ home.brand.slogan }}</p>
    <div v-if="home.notices?.length" class="notices">
      <p v-for="item in home.notices" :key="item.id">
        <span>{{ item.content }}</span>
        <button type="button" @click="dismiss(item.id)">知道了</button>
      </p>
    </div>

    <article class="hero">
      <p class="eyebrow">表达训练</p>
      <h1>让表达更自信</h1>
      <p>完成一次练习，发现自己的进步</p>
      <button type="button" @click="router.push('/topics')">开始训练</button>
    </article>

    <div class="stats">
      <article class="card stat">
        <p>今日训练</p>
        <strong>{{ home.weeklyDone }}/{{ home.weeklyTarget }} <span>次</span></strong>
        <div class="track" aria-hidden="true">
          <i :style="{ width: progressPercent + '%' }"></i>
        </div>
        <small>本周目标 {{ home.weeklyTarget }} 次</small>
      </article>
      <article class="card stat">
        <p>最近成绩</p>
        <strong>{{ home.latest ? home.latest.score : '—' }} <span>分</span></strong>
        <em v-if="deltaText">较上次 {{ deltaText }}</em>
        <small>{{ home.latest?.date }}</small>
      </article>
    </div>

    <article class="card topic">
      <div class="row-title">
        <h2>推荐训练</h2>
        <button v-if="home.recommended" type="button" @click="router.push(`/practice/${home.recommended.id}`)">
          去练习
        </button>
      </div>
      <template v-if="home.recommended">
        <p>{{ home.recommended.title }}</p>
        <ul>
          <li>{{ home.recommended.category }}</li>
          <li>{{ home.recommended.difficulty }}</li>
          <li>{{ durationLabel(home.recommended) }}</li>
        </ul>
      </template>
    </article>

    <article v-if="home.latest" class="card recent">
      <h2>最近一次训练</h2>
      <button type="button" class="record" @click="router.push(`/report/${home.latest.reportId}`)">
        <span>
          <strong>{{ home.latest.title }}</strong>
          <small>{{ home.latest.date }} · {{ home.latest.duration }}</small>
        </span>
        <b>{{ home.latest.score }}</b>
      </button>
    </article>
    </template>
  </PhoneFrame>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 8px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 16px;
  line-height: 1.2;
}

.brand small {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.mark,
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
}

.mark {
  color: #fff;
  background: var(--brand);
}

.mark svg {
  width: 22px;
  height: 22px;
}

.avatar {
  position: relative;
  border: 0;
  color: var(--brand);
  background: #e8ebf8;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  overflow: hidden;
}

.avatar img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.notices {
  margin-top: 12px;
}

.notices p {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 8px 0 0;
  padding: 10px 12px;
  border-radius: 12px;
  background: #eef0fa;
  font-size: 13px;
  line-height: 1.45;
}

.notices button {
  flex: none;
  border: 0;
  padding: 0;
  color: var(--brand);
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.welcome {
  margin: 12px 0 0;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.35;
}

.missing {
  margin: 16px 0 0;
  color: var(--muted);
  text-align: center;
}

.slogan,
.eyebrow,
.hero p,
.stat p,
.stat small,
.record small {
  margin: 0;
}

.slogan {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.hero {
  margin-top: 16px;
  padding: 20px;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(145deg, #3949ab 0%, #5b54c8 100%);
  box-shadow: 0 12px 28px rgba(57, 73, 171, 0.28);
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.04em;
  opacity: 0.85;
}

.hero h1 {
  margin: 8px 0 0;
  font-size: 24px;
  font-weight: 700;
}

.hero p {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.5;
  opacity: 0.92;
}

.hero button {
  width: 100%;
  height: 44px;
  margin-top: 16px;
  border: 0;
  border-radius: 12px;
  color: var(--brand);
  background: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.stat {
  padding: 16px;
}

.stat p,
.record small,
.stat small {
  color: var(--muted);
  font-size: 12px;
}

.stat strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
}

.stat strong span {
  font-size: 13px;
  font-weight: 600;
}

.track {
  height: 6px;
  margin-top: 12px;
  overflow: hidden;
  border-radius: 99px;
  background: #e7ebf5;
}

.track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--brand);
}

.stat small,
.stat em {
  display: block;
  margin-top: 8px;
}

.stat em {
  color: var(--ok);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.topic,
.recent {
  margin-top: 12px;
  padding: 16px;
}

.row-title,
.record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

h2 {
  margin: 0;
  font-size: 16px;
}

.row-title button {
  border: 0;
  padding: 0;
  color: var(--brand);
  background: transparent;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.topic p {
  margin: 10px 0 0;
  font-size: 15px;
  line-height: 1.55;
}

.topic ul {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.topic li {
  padding: 4px 8px;
  border-radius: 999px;
  color: var(--brand);
  background: #eef0fa;
  font-size: 12px;
}

.record {
  width: 100%;
  margin-top: 12px;
  padding: 0;
  border: 0;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.record strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
}

.record small {
  display: block;
  margin-top: 4px;
}

.record b {
  flex: none;
  color: var(--ai);
  font-size: 22px;
}
</style>
