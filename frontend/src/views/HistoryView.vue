<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PhoneFrame from '@/components/PhoneFrame.vue'
import { api } from '@/api/http'

const router = useRouter()
const showEmpty = ref(false)
const history = ref(null)
const error = ref('')
const records = computed(() => history.value?.records ?? [])
const stats = computed(() => history.value?.stats)
const scored = computed(() => records.value.filter((item) => item.status === 'completed' && item.score != null))
const trend = computed(() => [...scored.value].reverse())

function statusLabel(item) {
  const names = { recording: '录制中', paused: '已暂停', analyzing: '分析中', failed: '分析失败' }
  return names[item.status] || ''
}

function openRecord(item) {
  if (item.reportId) router.push(`/report/${item.reportId}`)
  else router.push(`/practice/${item.topicId}`)
}

const points = computed(() => {
  const scores = trend.value.map((item) => item.score)
  if (scores.length === 0) return []
  const low = Math.min(...scores) - 8
  const high = Math.max(...scores) + 8
  const span = Math.max(high - low, 1)
  const step = scores.length === 1 ? 0 : 228 / (scores.length - 1)
  return trend.value.map((item, index) => {
    const x = 36 + index * step
    const y = 78 - ((item.score - low) / span) * 52
    return { ...item, x, y }
  })
})

const line = computed(() => points.value.map((item) => `${item.x},${item.y}`).join(' '))

onMounted(async () => {
  try {
    history.value = await api('/api/history')
  } catch (err) {
    error.value = err.message
  }
})
</script>

<template>
  <PhoneFrame label="训练历史">
    <div class="title-row">
      <h1 class="screen-title">训练历史</h1>
      <button type="button" @click="showEmpty = !showEmpty">
        {{ showEmpty ? '返回记录' : '查看空状态' }}
      </button>
    </div>

    <p v-if="error" class="empty-note">{{ error }}</p>

    <template v-if="!showEmpty && (records.length || scored.length)">
      <div class="stats">
        <article class="card">
          <span>累计训练</span>
          <strong>{{ stats.count }}</strong>
          <small>次</small>
        </article>
        <article class="card">
          <span>累计时长</span>
          <strong>{{ stats.durationText }}</strong>
          <small>已完成</small>
        </article>
        <article class="card">
          <span>平均得分</span>
          <strong>{{ stats.average }}</strong>
          <small>分</small>
        </article>
      </div>

      <section v-if="points.length" class="card trend">
        <h2>成绩趋势</h2>
        <svg viewBox="0 0 300 110" role="img" aria-label="已完成训练的成绩趋势">
          <polyline :points="line" />
          <g v-for="item in points" :key="item.sessionId">
            <circle :cx="item.x" :cy="item.y" r="4.5" />
            <text :x="item.x" :y="item.y - 12" text-anchor="middle">{{ item.score }}</text>
            <text :x="item.x" y="102" text-anchor="middle" class="date">{{ item.shortDate }}</text>
          </g>
        </svg>
      </section>

      <button
        v-for="item in records"
        :key="item.sessionId"
        type="button"
        class="card record"
        @click="openRecord(item)"
      >
        <span>
          <strong>{{ item.title }}</strong>
          <small>
            <em v-if="statusLabel(item)">{{ statusLabel(item) }}</em>
            {{ item.date }} · {{ item.duration }}
            <template v-if="item.failReason"> · {{ item.failReason }}</template>
          </small>
        </span>
        <b v-if="item.score != null">{{ item.score }}</b>
      </button>
    </template>

    <section v-else-if="!showEmpty && history && history.keepHistory === false" class="empty">
      <h2>已关闭训练历史</h2>
      <p>记录仍保存在数据库里。重新打开「保留训练历史」后会再次显示。清空才会永久删除。</p>
    </section>

    <section v-else-if="showEmpty || history" class="empty">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="8" />
          <path d="M12 8v4.5l3 2" />
        </svg>
      </span>
      <h2>还没有训练记录</h2>
      <p>完成一次练习后，成绩会出现在这里。</p>
      <button type="button" class="btn btn-primary" @click="router.push('/topics')">去题库看看</button>
    </section>
  </PhoneFrame>
</template>

<style scoped>
.title-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.title-row button {
  margin-bottom: 4px;
  border: 0;
  padding: 0;
  color: var(--brand);
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 16px;
}

.stats article {
  padding: 12px 8px;
  text-align: center;
}

.stats span,
.stats small,
.record small {
  color: var(--muted);
  font-size: 12px;
}

.stats strong {
  display: block;
  margin: 6px 0 2px;
  font-size: 18px;
}

.trend {
  margin-top: 12px;
  padding: 16px 12px 8px;
}

h2 {
  margin: 0 12px;
  font-size: 16px;
}

svg {
  width: 100%;
  height: auto;
  margin-top: 8px;
}

polyline {
  fill: none;
  stroke: #3949ab;
  stroke-width: 2.5;
  stroke-linecap: round;
}

circle {
  fill: #fff;
  stroke: #7567e8;
  stroke-width: 2.5;
}

text {
  fill: #20263a;
  font-size: 12px;
  font-weight: 700;
}

text.date {
  fill: #65708a;
  font-size: 11px;
  font-weight: 500;
}

.record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  margin-top: 12px;
  padding: 16px;
  border: 0;
  text-align: left;
  cursor: pointer;
}

.record strong {
  display: block;
  font-size: 14px;
  line-height: 1.45;
}

.record small {
  display: block;
  margin-top: 4px;
}

.record em {
  margin-right: 6px;
  color: var(--brand);
  font-style: normal;
  font-weight: 700;
}

.record b {
  color: var(--ai);
  font-size: 22px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 72px;
  text-align: center;
}

.empty span {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  color: var(--brand);
  background: #e8ebf8;
}

.empty svg {
  width: 30px;
  height: 30px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
}

.empty h2 {
  margin: 16px 0 0;
}

.empty p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.empty .btn {
  width: 160px;
  margin-top: 16px;
}
</style>
