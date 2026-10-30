<script setup>
import { onMounted, ref } from 'vue'
import { accountName, api } from '../api'

const users = ref([])
const keyword = ref('')
const editing = ref(null)
const error = ref('')
const me = accountName()

async function load() {
  const query = keyword.value.trim() ? `?keyword=${encodeURIComponent(keyword.value.trim())}` : ''
  users.value = await api(`/api/admin/users${query}`)
}

onMounted(async () => {
  try {
    await load()
  } catch (err) {
    error.value = err.message
  }
})

function edit(user) {
  editing.value = { ...user }
}

async function save() {
  error.value = ''
  try {
    const saved = await api(`/api/admin/users/${editing.value.id}`, {
      method: 'PUT',
      body: {
        nickname: editing.value.nickname,
        school: editing.value.school,
        major: editing.value.major,
        bio: editing.value.bio,
        role: editing.value.role,
      },
    })
    const index = users.value.findIndex((item) => item.id === saved.id)
    if (index >= 0) users.value[index] = saved
    editing.value = null
  } catch (err) {
    error.value = err.message
  }
}

async function remove(user) {
  if (!window.confirm(`删除 ${user.nickname} 后，这个账号的训练和反馈也会删除。`)) return
  error.value = ''
  try {
    await api(`/api/admin/users/${user.id}`, { method: 'DELETE' })
    users.value = users.value.filter((item) => item.id !== user.id)
    if (editing.value?.id === user.id) editing.value = null
  } catch (err) {
    error.value = err.message
  }
}
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">用户</p>
        <h1>学员和管理员</h1>
      </div>
      <form class="btn-row" @submit.prevent="load">
        <input v-model="keyword" placeholder="账号或昵称" style="border:1px solid var(--line);border-radius:10px;padding:8px 12px" />
        <button class="btn" type="submit">搜索</button>
      </form>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="grid two">
      <article class="card">
        <table class="table">
          <thead>
            <tr><th>账号</th><th>昵称</th><th>角色</th><th>训练</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.loginName }}<p class="muted">{{ user.school }} {{ user.major }}</p></td>
              <td>{{ user.nickname }}</td>
              <td><span class="tag" :class="user.role === 'admin' ? 'warn' : ''">{{ user.role === 'admin' ? '管理员' : '学员' }}</span></td>
              <td>{{ user.sessionCount }}</td>
              <td>
                <button class="btn" type="button" @click="edit(user)">编辑</button>
                <button v-if="user.loginName !== me" class="btn danger" type="button" @click="remove(user)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!users.length" class="empty">没有找到用户。</p>
      </article>
      <form v-if="editing" class="card" @submit.prevent="save">
        <h2>编辑 {{ editing.loginName }}</h2>
        <p class="muted">注册于 {{ editing.createdAt }}</p>
        <label class="field">昵称<input v-model="editing.nickname" maxlength="32" required /></label>
        <label class="field">学校<input v-model="editing.school" maxlength="64" /></label>
        <label class="field">专业<input v-model="editing.major" maxlength="64" /></label>
        <label class="field">简介<textarea v-model="editing.bio" maxlength="120" rows="3"></textarea></label>
        <label class="field">
          角色
          <select v-model="editing.role">
            <option value="user">学员</option>
            <option value="admin">管理员</option>
          </select>
        </label>
        <button class="btn primary" type="submit">保存</button>
      </form>
    </div>
  </section>
</template>
