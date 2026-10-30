<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import PhoneFrame from '@/components/PhoneFrame.vue'
import { api, avatarObjectUrl, setToken } from '@/api/http'
import { useAppStore } from '@/stores/app'

const app = useAppStore()
const panel = ref('')
const profile = ref(null)
const help = ref(null)
const error = ref('')
const avatarUrl = ref('')
const summary = ref({ participants: 0, weeklySessions: 0 })
const feedbacks = ref([])
const feedbackCategory = ref('功能建议')
const feedbackContent = ref('')
const draft = ref({ nickname: '', school: '', major: '', bio: '' })
const favorites = computed(() => app.topics.filter((item) => item.favorite))

const menus = computed(() => [
  { id: 'favorites', label: '我的收藏', note: '已收藏的训练题目' },
  { id: 'goal', label: '训练目标', note: '每周练习次数' },
  { id: 'notice', label: '通知设置', note: '提醒与报告通知' },
  { id: 'privacy', label: '隐私与数据管理', note: '训练记录如何保留' },
  { id: 'help', label: '帮助与反馈', note: '使用说明和意见' },
  { id: 'about', label: '关于声析', note: help.value?.about.en || 'VOXINSIGHT' },
])

const titles = {
  favorites: '我的收藏',
  goal: '训练目标',
  notice: '通知设置',
  privacy: '隐私与数据管理',
  help: '帮助与反馈',
  about: '关于声析',
  edit: '编辑资料',
}

function openEdit() {
  draft.value = {
    nickname: profile.value?.user.nickname || '',
    school: profile.value?.user.school || '',
    major: profile.value?.user.major || '',
    bio: profile.value?.user.bio || '',
  }
  panel.value = 'edit'
}

async function refreshAvatar() {
  if (avatarUrl.value) {
    URL.revokeObjectURL(avatarUrl.value)
    avatarUrl.value = ''
  }
  if (profile.value?.user.hasAvatar) {
    avatarUrl.value = await avatarObjectUrl()
  }
}

async function loadSummary() {
  summary.value = await api('/api/profile/summary')
}

async function changeHistory(event) {
  error.value = ''
  try {
    await app.saveSettings({ keepHistory: event.target.checked })
    profile.value = await api('/api/profile')
  } catch (err) {
    event.target.checked = app.keepHistory
    error.value = err.message
  }
}

async function changeAnonymous(event) {
  error.value = ''
  try {
    await app.saveSettings({ anonymousStats: event.target.checked })
    await loadSummary()
  } catch (err) {
    event.target.checked = app.anonymousStats
    error.value = err.message
  }
}

onMounted(async () => {
  try {
    await app.load()
    profile.value = await api('/api/profile')
    help.value = await api('/api/help')
    feedbacks.value = await api('/api/feedback')
    await Promise.all([refreshAvatar(), loadSummary()])
  } catch (err) {
    error.value = err.message
  }
})

onUnmounted(() => {
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value)
})

async function saveProfile() {
  error.value = ''
  try {
    profile.value = await api('/api/profile', { method: 'PUT', body: draft.value })
    panel.value = ''
  } catch (err) {
    error.value = err.message
  }
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 512 * 1024) {
    error.value = '头像需要是 512KB 以内的图片。'
    return
  }
  const form = new FormData()
  form.append('avatar', file)
  error.value = ''
  try {
    await api('/api/profile/avatar', { method: 'POST', form })
    profile.value = await api('/api/profile')
    await refreshAvatar()
  } catch (err) {
    error.value = err.message
  }
}

async function sendFeedback() {
  error.value = ''
  try {
    await api('/api/feedback', {
      method: 'POST',
      body: { category: feedbackCategory.value, content: feedbackContent.value },
    })
    feedbackContent.value = ''
    feedbacks.value = await api('/api/feedback')
  } catch (err) {
    error.value = err.message
  }
}

async function clearHistory() {
  if (!window.confirm('清空后无法恢复。确定删除全部训练记录？')) return
  error.value = ''
  try {
    await api('/api/history', { method: 'DELETE' })
    profile.value = await api('/api/profile')
  } catch (err) {
    error.value = err.message
  }
}

async function logout() {
  try {
    await api('/api/auth/logout', { method: 'POST' })
  } catch {
    // 令牌失效时仍然清除本地登录态
  }
  setToken(null)
  location.assign('/login')
}
</script>

<template>
  <PhoneFrame :tabs="!panel" label="个人中心">
    <template v-if="panel" #bar>
      <PageHeader :title="titles[panel]" />
      <button class="back" type="button" aria-label="返回" @click="panel = ''">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5 8 12l7 7" /></svg>
      </button>
    </template>

    <template v-if="!panel && profile">
      <h1 class="screen-title">个人中心</h1>
      <section class="card profile">
        <img v-if="avatarUrl" class="avatar photo" :src="avatarUrl" alt="" />
        <span v-else class="avatar">{{ profile.user.initial }}</span>
        <div>
          <p class="name">{{ profile.user.nickname }}</p>
          <p>{{ profile.user.school || '未填写学校' }} · {{ profile.user.major || '未填写专业' }}</p>
          <small>{{ profile.user.bio }}</small>
          <small>账号 {{ profile.user.loginName }}</small>
        </div>
      </section>
      <div class="profile-actions">
        <button type="button" @click="openEdit">编辑资料</button>
        <label class="upload">
          更换头像
          <input type="file" accept="image/jpeg,image/png,image/webp" @change="uploadAvatar" />
        </label>
        <button type="button" @click="logout">退出登录</button>
      </div>

      <div class="stats">
        <article class="card">
          <strong>{{ profile.stats.count }}</strong>
          <span>累计训练</span>
        </article>
        <article class="card">
          <strong>{{ profile.stats.durationText }}</strong>
          <span>累计时长</span>
        </article>
        <article class="card">
          <strong>{{ profile.stats.average }}</strong>
          <span>平均成绩</span>
        </article>
      </div>

      <button type="button" class="card goal" @click="panel = 'goal'">
        <span>训练目标</span>
        <strong>每周练习 {{ app.weeklyTarget }} 次</strong>
        <small>本周已完成 {{ profile.weeklyDone }}/{{ app.weeklyTarget }}</small>
      </button>

      <div class="card menu">
        <button v-for="item in menus" :key="item.id" type="button" @click="panel = item.id">
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.note }}</small>
          </span>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 6 6-6 6" /></svg>
        </button>
      </div>
    </template>

    <template v-else-if="panel === 'favorites'">
      <p v-if="favorites.length === 0" class="muted">还没有收藏题目。</p>
      <article v-for="topic in favorites" :key="topic.id" class="card line">
        <RouterLink :to="`/practice/${topic.id}`">{{ topic.title }}</RouterLink>
        <button type="button" @click="app.toggleFavorite(topic.id)">取消收藏</button>
      </article>
    </template>

    <section v-else-if="panel === 'goal'" class="card pad">
      <h2>每周练习次数</h2>
      <div class="stepper">
        <button type="button" @click="app.setWeeklyTarget(app.weeklyTarget - 1)">－</button>
        <strong>{{ app.weeklyTarget }}</strong>
        <button type="button" @click="app.setWeeklyTarget(app.weeklyTarget + 1)">＋</button>
      </div>
      <p>首页进度会按这个目标显示。当前本周已完成 {{ profile?.weeklyDone ?? 0 }} 次。</p>
    </section>

    <section v-else-if="panel === 'notice'" class="card menu">
      <label><span>练习提醒</span><input v-model="app.reminderOn" type="checkbox" /></label>
      <label class="time">
        <span>提醒时间</span>
        <input v-model="app.reminderTime" type="time" />
      </label>
      <label><span>报告生成通知</span><input v-model="app.reportNoticeOn" type="checkbox" /></label>
      <label><span>周目标提醒</span><input v-model="app.goalNoticeOn" type="checkbox" /></label>
      <p class="note">通知出现在首页，不会发到手机或邮箱。</p>
    </section>

    <section v-else-if="panel === 'privacy'" class="card menu">
      <label><span>保留训练历史</span><input :checked="app.keepHistory" type="checkbox" @change="changeHistory" /></label>
      <label><span>允许匿名使用统计</span><input :checked="app.anonymousStats" type="checkbox" @change="changeAnonymous" /></label>
      <p class="note">
        关闭历史后，首页、历史和个人统计不再显示记录，重新打开会恢复。清空才会永久删除。
      </p>
      <p class="note">
        已有 {{ summary.participants }} 人允许匿名统计，本周合计 {{ summary.weeklySessions }} 次训练。这里不显示昵称。
      </p>
      <button type="button" class="danger" @click="clearHistory">清空训练记录</button>
    </section>

    <section v-else-if="panel === 'help'">
      <article v-for="article in help?.articles || []" :key="article.title" class="card pad">
        <h2>{{ article.title }}</h2>
        <p>{{ article.content }}</p>
      </article>
      <form class="card pad form" @submit.prevent="sendFeedback">
        <h2>提交反馈</h2>
        <label>
          类别
          <select v-model="feedbackCategory">
            <option>功能建议</option>
            <option>问题反馈</option>
            <option>其他</option>
          </select>
        </label>
        <label>
          内容
          <textarea v-model="feedbackContent" maxlength="500" rows="4" required></textarea>
        </label>
        <button class="btn btn-primary" type="submit">提交</button>
      </form>
      <article v-for="item in feedbacks" :key="item.createdAt + item.content" class="card pad">
        <h2>{{ item.category }} · {{ item.status }}</h2>
        <p>{{ item.content }}</p>
        <p v-if="item.reply">回复：{{ item.reply }}</p>
        <small>{{ item.createdAt }}<template v-if="item.repliedAt"> · 回复于 {{ item.repliedAt }}</template></small>
      </article>
    </section>

    <form v-else-if="panel === 'edit'" class="card pad form" @submit.prevent="saveProfile">
      <label>昵称<input v-model="draft.nickname" maxlength="32" required /></label>
      <label>学校<input v-model="draft.school" maxlength="64" /></label>
      <label>专业<input v-model="draft.major" maxlength="64" /></label>
      <label>简介<textarea v-model="draft.bio" maxlength="120" rows="3"></textarea></label>
      <button class="btn btn-primary" type="submit">保存</button>
    </form>

    <section v-else-if="help" class="card pad about">
      <strong>{{ help.about.zh }} {{ help.about.en }}</strong>
      <p>{{ help.about.slogan }}</p>
      <p>{{ help.about.about }}</p>
      <small>版本 {{ help.about.version }}</small>
    </section>
    <p v-if="error" class="muted">{{ error }}</p>
  </PhoneFrame>
</template>

<style scoped>
.profile {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
}

.avatar {
  width: 52px;
  height: 52px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 16px;
  color: var(--brand);
  background: #e8ebf8;
  font-size: 20px;
  font-weight: 700;
}

.avatar.photo {
  object-fit: cover;
}

.profile-actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}

.profile-actions button,
.upload {
  border: 0;
  padding: 0;
  color: var(--brand);
  background: transparent;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.upload input {
  display: none;
}

.name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.profile p,
.profile small,
.goal small,
.menu small,
.note,
.muted,
.about small {
  color: var(--muted);
  font-size: 12px;
}

.profile p,
.profile small,
.goal small {
  display: block;
  margin-top: 4px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.stats article {
  padding: 12px 8px;
  text-align: center;
}

.stats strong {
  display: block;
  font-size: 16px;
}

.stats span {
  color: var(--muted);
  font-size: 12px;
}

.goal {
  display: block;
  width: 100%;
  margin-top: 12px;
  padding: 16px;
  border: 0;
  text-align: left;
  cursor: pointer;
}

.goal span {
  color: var(--muted);
  font-size: 12px;
}

.goal strong {
  display: block;
  margin-top: 6px;
  font-size: 16px;
}

.menu {
  margin-top: 12px;
  overflow: hidden;
}

.menu button,
.menu label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid #eef1f7;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.menu button:last-child,
.menu label:last-child {
  border-bottom: 0;
}

.menu strong {
  display: block;
  font-size: 15px;
}

.menu svg,
.back svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: var(--muted);
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.back {
  position: absolute;
  top: 14px;
  left: 12px;
  z-index: 2;
  width: 36px;
  height: 36px;
  border: 0;
  color: var(--text);
  background: transparent;
  cursor: pointer;
}

.line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding: 16px;
}

.line a {
  color: var(--text);
  font-size: 14px;
  line-height: 1.5;
  text-decoration: none;
}

.line button,
.stepper button {
  border: 0;
  color: var(--brand);
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.pad {
  margin-top: 12px;
  padding: 16px;
}

.pad h2 {
  margin: 0;
  font-size: 16px;
}

.pad p,
.about p {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.6;
}

.stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
}

.stepper button {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: #eef0fa;
  font-size: 20px;
}

.stepper strong {
  font-size: 32px;
}

.note {
  margin: 0;
  padding: 0 16px 14px;
  line-height: 1.5;
}

.about strong {
  font-size: 18px;
}

.muted {
  margin-top: 20px;
  text-align: center;
}

.time input {
  border: 1px solid #e3e8f2;
  border-radius: 8px;
  padding: 6px 8px;
  font: inherit;
}

.danger {
  width: calc(100% - 32px);
  margin: 0 16px 16px;
  height: 40px;
  border: 0;
  border-radius: 12px;
  color: #c62828;
  background: #fdecea;
  font-weight: 700;
  cursor: pointer;
}

.form label {
  display: block;
  margin-top: 12px;
  color: var(--muted);
  font-size: 13px;
}

.form input,
.form textarea,
.form select {
  width: 100%;
  margin-top: 6px;
  padding: 10px 12px;
  border: 1px solid #e3e8f2;
  border-radius: 10px;
  font: inherit;
  color: var(--text);
}

.form .btn {
  width: 100%;
  margin-top: 16px;
}
</style>
