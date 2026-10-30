<script setup>
import { useRoute, useRouter } from 'vue-router'
import { api, setSession } from '../api'

const route = useRoute()
const router = useRouter()

function on(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

async function logout() {
  try {
    await api('/api/auth/logout', { method: 'POST' })
  } catch {
    // 令牌已经失效时，本地仍然退出。
  }
  setSession(null)
  await router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="side">
      <div class="brand">
        <span class="mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <path d="M4 14.5c1.2-1.6 2.2-2.4 3.2-2.4 1.4 0 1.6 3.2 3 3.2s1.7-6.3 3.2-6.3 1.8 4.2 3.1 4.2 2-.8 3.3-2.2" stroke="#fff" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </span>
        <span><strong>声析</strong><small>管理后台</small></span>
      </div>
      <nav class="nav">
        <RouterLink to="/" custom v-slot="{ href, navigate }">
          <a :href="href" :class="{ 'router-link-active': route.path === '/' }" @click="navigate">概览</a>
        </RouterLink>
        <RouterLink to="/users" :class="{ active: on('/users') }">用户</RouterLink>
        <RouterLink to="/topics" :class="{ active: on('/topics') }">题目</RouterLink>
        <RouterLink to="/sessions" :class="{ active: on('/sessions') }">训练记录</RouterLink>
        <RouterLink to="/feedbacks" :class="{ active: on('/feedbacks') }">反馈</RouterLink>
      </nav>
      <button class="exit" type="button" @click="logout">退出登录</button>
    </aside>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>
