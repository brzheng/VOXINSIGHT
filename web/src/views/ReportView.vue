<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, token } from '../api'

const route = useRoute()
const router = useRouter()
const report = ref(null)
const error = ref('')
const audioUrl = ref('')

onMounted(async () => {
  try {
    report.value = await api(`/api/reports/${route.params.reportId}`)
    if (report.value.hasAudio) {
      const response = await fetch(`/api/reports/${route.params.reportId}/audio`, {
        headers: { Authorization: `Bearer ${token()}` },
      })
      if (response.ok) audioUrl.value = URL.createObjectURL(await response.blob())
    }
  } catch (err) {
    error.value = err.message
  }
})

onUnmounted(() => {
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
})
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">分析报告</p>
        <h1>{{ report?.title || '报告' }}</h1>
        <p v-if="report" class="lead">{{ report.date }} · {{ report.duration }} · {{ report.label }}</p>
      </div>
      <button class="btn" type="button" @click="router.push(report ? `/practice/${report.topicId}` : '/topics')">再练一次</button>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="report">
      <article class="card">
        <span class="muted">总分</span>
        <div class="score">{{ report.score }}</div>
        <div class="metrics">
          <div class="metric"><span>语速</span><b>{{ report.speechRate }}</b></div>
          <div class="metric"><span>停顿</span><b>{{ report.pauseCount }}</b></div>
          <div class="metric"><span>流畅度</span><b>{{ report.fluency }}</b></div>
          <div class="metric"><span>内容分</span><b>{{ report.contentScore ?? '—' }}</b></div>
        </div>
      </article>
      <div class="grid two" style="margin-top:16px">
        <article class="card">
          <h2>识别文本</h2>
          <p>{{ report.transcript || '这次报告没有识别文本。' }}</p>
          <p v-if="report.contentSummary" class="muted">{{ report.contentSummary }}</p>
          <audio v-if="audioUrl" :src="audioUrl" controls style="width:100%;margin-top:12px"></audio>
          <p v-else class="muted">这次练习没有保存录音。</p>
        </article>
        <div class="grid">
          <article class="card">
            <h2>表现较好</h2>
            <p v-for="(line, index) in report.strengths" :key="'s' + index">{{ line }}</p>
          </article>
          <article class="card">
            <h2>可以改进</h2>
            <p v-for="(line, index) in report.improvements" :key="'i' + index">{{ line }}</p>
          </article>
          <article class="card">
            <h2>建议</h2>
            <p v-for="(line, index) in report.suggestions" :key="'g' + index">{{ line }}</p>
          </article>
        </div>
      </div>
    </template>
  </section>
</template>
