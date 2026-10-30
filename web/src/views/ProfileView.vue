<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const profile = ref(null)
const help = ref(null)
const feedbacks = ref([])
const draft = ref({ nickname: '', school: '', major: '', bio: '' })
const goal = ref(3)
const category = ref('功能建议')
const content = ref('')
const error = ref('')
const message = ref('')

async function load() {
  const [me, about, notes] = await Promise.all([
    api('/api/profile'),
    api('/api/help'),
    api('/api/feedback'),
  ])
  profile.value = me
  help.value = about
  feedbacks.value = notes
  draft.value = {
    nickname: me.user.nickname,
    school: me.user.school || '',
    major: me.user.major || '',
    bio: me.user.bio || '',
  }
  goal.value = me.weeklyTarget
}

onMounted(async () => {
  try {
    await load()
  } catch (err) {
    error.value = err.message
  }
})

async function saveProfile() {
  error.value = ''
  message.value = ''
  try {
    profile.value = await api('/api/profile', { method: 'PUT', body: draft.value })
    message.value = '资料已保存。'
  } catch (err) {
    error.value = err.message
  }
}

async function saveGoal() {
  error.value = ''
  message.value = ''
  try {
    await api('/api/profile/goal', { method: 'PUT', body: { targetCount: Number(goal.value) } })
    profile.value = await api('/api/profile')
    message.value = '每周目标已更新。'
  } catch (err) {
    error.value = err.message
  }
}

async function sendFeedback() {
  error.value = ''
  message.value = ''
  try {
    await api('/api/feedback', { method: 'POST', body: { category: category.value, content: content.value } })
    content.value = ''
    feedbacks.value = await api('/api/feedback')
    message.value = '反馈已提交。'
  } catch (err) {
    error.value = err.message
  }
}
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">个人中心</p>
        <h1>{{ profile?.user.nickname || '我的资料' }}</h1>
        <p v-if="profile" class="lead">账号 {{ profile.user.loginName }}</p>
      </div>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="message" class="muted">{{ message }}</p>
    <div v-if="profile" class="grid two">
      <div class="grid">
        <article class="card">
          <div class="metrics">
            <div class="metric"><span>完成次数</span><b>{{ profile.stats.count }}</b></div>
            <div class="metric"><span>累计时长</span><b>{{ profile.stats.durationText }}</b></div>
            <div class="metric"><span>平均分</span><b>{{ profile.stats.average ?? '—' }}</b></div>
            <div class="metric"><span>本周</span><b>{{ profile.weeklyDone }}/{{ profile.weeklyTarget }}</b></div>
          </div>
        </article>
        <form class="card" @submit.prevent="saveProfile">
          <h2>资料</h2>
          <label class="field">昵称<input v-model="draft.nickname" maxlength="32" required /></label>
          <label class="field">学校<input v-model="draft.school" maxlength="64" /></label>
          <label class="field">专业<input v-model="draft.major" maxlength="64" /></label>
          <label class="field">简介<textarea v-model="draft.bio" maxlength="120" rows="3"></textarea></label>
          <button class="btn primary" type="submit">保存资料</button>
        </form>
        <form class="card" @submit.prevent="saveGoal">
          <h2>每周目标</h2>
          <label class="field">
            每周练习次数（1–7）
            <input v-model.number="goal" type="number" min="1" max="7" required />
          </label>
          <button class="btn primary" type="submit">保存目标</button>
        </form>
      </div>
      <div class="grid">
        <form class="card" @submit.prevent="sendFeedback">
          <h2>反馈</h2>
          <label class="field">
            类别
            <select v-model="category">
              <option>功能建议</option>
              <option>问题反馈</option>
              <option>其他</option>
            </select>
          </label>
          <label class="field">内容<textarea v-model="content" maxlength="500" rows="4" required></textarea></label>
          <button class="btn primary" type="submit">提交</button>
        </form>
        <article v-for="item in feedbacks" :key="item.createdAt + item.content" class="card">
          <h2>{{ item.category }} · {{ item.status }}</h2>
          <p>{{ item.content }}</p>
          <p v-if="item.reply">回复：{{ item.reply }}</p>
          <p class="muted">{{ item.createdAt }}<template v-if="item.repliedAt"> · 回复于 {{ item.repliedAt }}</template></p>
        </article>
        <article v-if="help" class="card">
          <h2>{{ help.about.zh }} {{ help.about.en }}</h2>
          <p>{{ help.about.slogan }}</p>
          <p class="muted">{{ help.about.about }}</p>
        </article>
      </div>
    </div>
  </section>
</template>
