<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const categoryName = ref('')
const error = ref('')
const pending = ref(false)
const form = reactive({
  categoryId: '',
  title: '',
  difficulty: '入门',
  durationMin: 1,
  durationMax: 2,
  guide: '尽量自然表达，注意语速与停顿。',
  recommended: false,
  sortOrder: 1,
  checkpoints: [{ label: '', keywords: '' }],
})

const editing = route.params.id

onMounted(async () => {
  try {
    categories.value = await api('/api/admin/categories')
    if (editing) {
      const topic = await api(`/api/admin/topics/${editing}`)
      form.categoryId = topic.categoryId
      form.title = topic.title
      form.difficulty = topic.difficulty
      form.durationMin = topic.durationMin
      form.durationMax = topic.durationMax
      form.guide = topic.guide
      form.recommended = topic.recommended
      form.sortOrder = topic.sortOrder
      form.checkpoints = topic.checkpoints.length ? topic.checkpoints.map((item) => ({ ...item })) : [{ label: '', keywords: '' }]
    } else if (categories.value.length) {
      form.categoryId = categories.value[0].id
      form.sortOrder = 10
    }
  } catch (err) {
    error.value = err.message
  }
})

async function addCategory() {
  error.value = ''
  try {
    const created = await api('/api/admin/categories', { method: 'POST', body: { name: categoryName.value } })
    categories.value.push(created)
    form.categoryId = created.id
    categoryName.value = ''
  } catch (err) {
    error.value = err.message
  }
}

async function save() {
  error.value = ''
  pending.value = true
  const body = {
    categoryId: Number(form.categoryId),
    title: form.title,
    difficulty: form.difficulty,
    durationMin: Number(form.durationMin),
    durationMax: Number(form.durationMax),
    guide: form.guide,
    recommended: form.recommended,
    sortOrder: Number(form.sortOrder),
    checkpoints: form.checkpoints,
  }
  try {
    if (editing) await api(`/api/admin/topics/${editing}`, { method: 'PUT', body })
    else await api('/api/admin/topics', { method: 'POST', body })
    await router.push('/topics')
  } catch (err) {
    error.value = err.message
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <section>
    <header class="page-head">
      <div>
        <p class="eyebrow">题目</p>
        <h1>{{ editing ? '编辑题目' : '新建题目' }}</h1>
      </div>
      <button class="btn" type="button" @click="router.push('/topics')">返回列表</button>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <form class="grid two" @submit.prevent="save">
      <div class="card">
        <label class="field">
          分类
          <select v-model="form.categoryId" required>
            <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <div class="btn-row" style="margin-bottom:12px">
          <input v-model="categoryName" maxlength="32" placeholder="新分类名称" style="border:1px solid var(--line);border-radius:10px;padding:8px 12px" />
          <button class="btn" type="button" @click="addCategory">添加分类</button>
        </div>
        <label class="field">标题<input v-model="form.title" maxlength="120" required /></label>
        <label class="field">
          难度
          <select v-model="form.difficulty">
            <option>入门</option>
            <option>进阶</option>
            <option>挑战</option>
          </select>
        </label>
        <div class="grid" style="grid-template-columns:1fr 1fr 1fr">
          <label class="field">最短分钟<input v-model.number="form.durationMin" type="number" min="1" max="30" required /></label>
          <label class="field">最长分钟<input v-model.number="form.durationMax" type="number" min="1" max="30" required /></label>
          <label class="field">排序<input v-model.number="form.sortOrder" type="number" min="0" max="255" required /></label>
        </div>
        <label class="field">录音提示<textarea v-model="form.guide" maxlength="120" rows="3" required></textarea></label>
        <label class="field" style="flex-direction:row;align-items:center;gap:8px">
          <input v-model="form.recommended" type="checkbox" />
          设为首页推荐题
        </label>
        <button class="btn primary" type="submit" :disabled="pending">保存题目</button>
      </div>
      <div class="card">
        <h2>内容要点</h2>
        <p class="muted">关键词用逗号分隔。识别文本命中其中一个词，这个要点就算覆盖。</p>
        <div v-for="(point, index) in form.checkpoints" :key="index" class="card" style="margin-bottom:10px;box-shadow:none">
          <label class="field">要点名称<input v-model="point.label" maxlength="40" required /></label>
          <label class="field">关键词<input v-model="point.keywords" maxlength="200" placeholder="例如：我是,我叫,名叫" required /></label>
          <button class="btn danger" type="button" @click="form.checkpoints.splice(index, 1)">删除要点</button>
        </div>
        <button class="btn" type="button" @click="form.checkpoints.push({ label: '', keywords: '' })">添加要点</button>
      </div>
    </form>
  </section>
</template>
