<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, setSession } from '../api'

const router = useRouter()
const loginName = ref('')
const password = ref('')
const error = ref('')
const pending = ref(false)

async function submit() {
  error.value = ''
  pending.value = true
  try {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: { loginName: loginName.value.trim(), password: password.value },
    })
    setSession(data.token, data.loginName)
    if (data.role !== 'admin') {
      try {
        await api('/api/auth/logout', { method: 'POST' })
      } catch {
        // 学员令牌会在下面清掉。
      }
      setSession(null)
      error.value = '这个账号没有管理权限。'
      return
    }
    await router.push('/')
  } catch (err) {
    setSession(null)
    error.value = err.message
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="login">
    <section class="login-copy">
      <p class="eyebrow">ADMIN</p>
      <h1>声析管理后台</h1>
      <p>管理学员、题目和内容要点，查看训练记录，并回复用户反馈。</p>
    </section>
    <section class="login-panel">
      <form class="card" @submit.prevent="submit">
        <h2>管理员登录</h2>
        <p v-if="error" class="error">{{ error }}</p>
        <label class="field">账号<input v-model="loginName" autocomplete="username" maxlength="32" required /></label>
        <label class="field">密码<input v-model="password" type="password" autocomplete="current-password" maxlength="32" required /></label>
        <button class="btn primary" type="submit" :disabled="pending">登录</button>
      </form>
    </section>
  </div>
</template>
