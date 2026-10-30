<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import PhoneFrame from '@/components/PhoneFrame.vue'
import { api, token } from '@/api/http'

const route = useRoute()
const report = ref(null)
const audioUrl = ref('')
const practiceTopicId = computed(() => report.value?.topicId)
const displayTitle = computed(() => report.value?.title)

async function loadAudio(reportId, hasAudio) {
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
  audioUrl.value = ''
  if (!hasAudio) return
  const response = await fetch(`/api/reports/${reportId}/audio`, {
    headers: { Authorization: `Bearer ${token()}` },
  })
  if (!response.ok) return
  audioUrl.value = URL.createObjectURL(await response.blob())
}

async function loadReport(reportId) {
  report.value = null
  try {
    report.value = await api(`/api/reports/${reportId}`)
    await loadAudio(reportId, report.value?.hasAudio)
  } catch {
    report.value = null
  }
}

onUnmounted(() => {
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
})

watch(() => route.params.reportId, loadReport, { immediate: true })
</script>

<template>
  <PhoneFrame :tabs="false" label="训练分析报告">
    <template #bar>
      <PageHeader title="训练分析报告" back-to="/history" />
    </template>

    <template v-if="report">
      <p class="badge">{{ report.label }}</p>
      <article class="card score">
        <div
          class="ring"
          :style="{ background: `conic-gradient(#3949ab ${report.score}%, #e7ebf5 0)` }"
          role="img"
          :aria-label="`表达分数 ${report.score} 分`"
        >
          <div>
            <strong>{{ report.score }}</strong>
            <span>表达</span>
          </div>
        </div>
        <p>{{ displayTitle }}</p>
        <small>{{ report.date }} · {{ report.duration }}</small>
      </article>

      <section v-if="report.contentScore != null" class="card block">
        <h2>内容分数 {{ report.contentScore }}</h2>
        <p>{{ report.contentSummary }}</p>
      </section>

      <section v-if="report.transcript" class="card block">
        <h2>识别文本</h2>
        <p class="transcript">{{ report.transcript }}</p>
      </section>

      <section class="card block recording">
        <h2>本次录音</h2>
        <audio
          v-if="audioUrl"
          :src="audioUrl"
          controls
          preload="metadata"
        ></audio>
        <p v-else class="no-audio">这次练习没有保存录音。</p>
      </section>

      <div class="metrics">
        <article class="card">
          <span>语速</span>
          <strong>{{ report.speechRate }}</strong>
          <small>字/分钟</small>
        </article>
        <article class="card">
          <span>停顿</span>
          <strong>{{ report.pauseCount }}</strong>
          <small>次</small>
        </article>
        <article class="card">
          <span>流畅度</span>
          <strong>{{ report.fluency }}</strong>
          <small>/100</small>
        </article>
      </div>

      <div class="bar-card card">
        <div>
          <span>表达流畅度</span>
          <b>{{ report.fluency }}/100</b>
        </div>
        <i><em :style="{ width: report.fluency + '%' }"></em></i>
      </div>

      <section class="card block">
        <h2>本次表现</h2>
        <p class="good">表现较好</p>
        <ul>
          <li v-for="item in report.strengths" :key="item">{{ item }}</li>
        </ul>
        <p class="improve">可改进</p>
        <ul>
          <li v-for="item in report.improvements" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section class="card block">
        <h2>改进建议</h2>
        <ol>
          <li v-for="item in report.suggestions" :key="item">{{ item }}</li>
        </ol>
      </section>
    </template>
    <p v-else class="missing">没有找到这份报告。</p>

    <template v-if="report" #dock>
      <RouterLink class="btn btn-ghost link" :to="`/practice/${practiceTopicId}`">再次练习</RouterLink>
      <RouterLink class="btn btn-primary link" to="/history">查看训练历史</RouterLink>
    </template>
  </PhoneFrame>
</template>

<style scoped>
.badge {
  display: inline-block;
  margin: 8px 0 0;
  padding: 4px 8px;
  border-radius: 999px;
  color: var(--ai);
  background: #efedfd;
  font-size: 12px;
  font-weight: 700;
}

.score {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 12px;
  padding: 20px 16px;
  text-align: center;
}

.ring {
  width: 132px;
  height: 132px;
  display: grid;
  place-items: center;
  border-radius: 50%;
}

.ring div {
  width: 104px;
  height: 104px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff;
}

.ring strong {
  font-size: 36px;
  line-height: 1;
}

.ring span,
.score small,
.metrics small {
  color: var(--muted);
  font-size: 12px;
}

.score p {
  margin: 14px 0 0;
  font-size: 15px;
  line-height: 1.5;
}

.score small {
  margin-top: 6px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.metrics article {
  padding: 12px 8px;
  text-align: center;
}

.metrics span,
.bar-card span {
  color: var(--muted);
  font-size: 12px;
}

.metrics strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
}

.bar-card,
.block {
  margin-top: 12px;
  padding: 16px;
}

.recording audio {
  width: 100%;
  margin-top: 12px;
}

.no-audio,
.transcript {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.transcript {
  color: var(--text);
  font-size: 14px;
  line-height: 1.6;
}

.bar-card div {
  display: flex;
  justify-content: space-between;
}

.bar-card b {
  font-size: 13px;
}

.bar-card i {
  display: block;
  height: 8px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 99px;
  background: #e7ebf5;
}

.bar-card em {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--ai);
}

h2 {
  margin: 0;
  font-size: 16px;
}

.good,
.improve {
  margin: 12px 0 0;
  font-size: 13px;
  font-weight: 700;
}

.good {
  color: var(--ok);
}

.improve {
  color: var(--warn);
}

ul,
ol {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--text);
  font-size: 14px;
  line-height: 1.55;
}

.link {
  flex: 1;
  display: grid;
  place-items: center;
  text-decoration: none;
}

.missing {
  margin-top: 24px;
  color: var(--muted);
  text-align: center;
}
</style>
