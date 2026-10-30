<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PhoneFrame from '@/components/PhoneFrame.vue'
import { api, setToken } from '@/api/http'

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
    if (mode.value === 'register' && nickname.value.trim()) {
      body.nickname = nickname.value.trim()
    }
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
  <PhoneFrame :tabs="false" label="登录">
    <h1 class="screen-title">{{ mode === 'login' ? '登录声析' : '注册账号' }}</h1>
    <form class="card form" @submit.prevent="submit">
      <label>
        账号
        <input v-model="loginName" autocomplete="username" maxlength="32" required />
      </label>
      <label v-if="mode === 'register'">
        昵称
        <input v-model="nickname" maxlength="32" placeholder="不填则使用账号" />
      </label>
      <label>
        密码
        <input v-model="password" type="password" autocomplete="current-password" maxlength="32" required />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="btn btn-primary" type="submit" :disabled="pending">
        {{ mode === 'login' ? '登录' : '注册并进入' }}
      </button>
      <button class="switch" type="button" @click="mode = mode === 'login' ? 'register' : 'login'">
        {{ mode === 'login' ? '没有账号，去注册' : '已有账号，去登录' }}
      </button>
    </form>
  </PhoneFrame>
</template>

<style scoped>
.form {
  margin-top: 16px;
  padding: 16px;
}

.form label {
  display: block;
  margin-top: 12px;
  color: var(--muted);
  font-size: 13px;
}

.form label:first-child {
  margin-top: 0;
}

.form input {
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

.switch {
  width: 100%;
  margin-top: 10px;
  border: 0;
  padding: 8px 0;
  color: var(--brand);
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.error {
  margin: 12px 0 0;
  color: #c62828;
  font-size: 13px;
}
</style>
