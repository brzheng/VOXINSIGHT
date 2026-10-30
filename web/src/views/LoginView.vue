<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, setToken } from '../api'

const router = useRouter()
const mode = ref('login')
const loginName = ref('')
const password = ref('')
const nickname = ref('')
const error = ref('')
const pending = ref(false)

async function submit() {
  error.value = ''
  pending.value = true
  try {
    const path = mode.value === 'login' ? '/api/auth/login' : '/api/auth/register'
    const body = { loginName: loginName.value.trim(), password: password.value }
    if (mode.value === 'register' && nickname.value.trim()) body.nickname = nickname.value.trim()
    const data = await api(path, { method: 'POST', body })
    setToken(data.token)
    await router.push('/')
  } catch (err) {
    error.value = err.message
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="login">
    <section class="login-copy">
      <p class="eyebrow">VOXINSIGHT</p>
      <h1>把一次表达，练成一份能回看的报告。</h1>
      <p>选题、录音、查看语速和停顿，再对照题目要点看内容有没有说全。</p>
    </section>
    <section class="login-panel">
      <form class="card" @submit.prevent="submit">
        <h2>{{ mode === 'login' ? '登录声析' : '注册账号' }}</h2>
        <p v-if="error" class="error">{{ error }}</p>
        <label class="field">
          账号
          <input v-model="loginName" autocomplete="username" maxlength="32" required />
        </label>
        <label v-if="mode === 'register'" class="field">
          昵称
          <input v-model="nickname" maxlength="32" placeholder="不填则使用账号" />
        </label>
        <label class="field">
          密码
          <input v-model="password" type="password" autocomplete="current-password" maxlength="32" required />
        </label>
        <div class="btn-row">
          <button class="btn primary" type="submit" :disabled="pending">
            {{ mode === 'login' ? '登录' : '注册并登录' }}
          </button>
          <button class="btn" type="button" @click="mode = mode === 'login' ? 'register' : 'login'">
            {{ mode === 'login' ? '没有账号，去注册' : '已有账号，去登录' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
