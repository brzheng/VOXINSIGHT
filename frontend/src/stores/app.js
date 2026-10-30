import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api/http'

export const useAppStore = defineStore('app', () => {
  const topics = ref([])
  const categories = ref(['全部'])
  const weeklyTarget = ref(3)
  const settings = ref({
    reminderOn: true,
    reportNoticeOn: true,
    goalNoticeOn: true,
    keepHistory: true,
    anonymousStats: false,
    reminderTime: '20:00',
  })

  const reminderOn = computed({
    get: () => settings.value.reminderOn,
    set: (value) => saveSettings({ reminderOn: value }),
  })
  const reportNoticeOn = computed({
    get: () => settings.value.reportNoticeOn,
    set: (value) => saveSettings({ reportNoticeOn: value }),
  })
  const goalNoticeOn = computed({
    get: () => settings.value.goalNoticeOn,
    set: (value) => saveSettings({ goalNoticeOn: value }),
  })
  const keepHistory = computed({
    get: () => settings.value.keepHistory,
    set: (value) => saveSettings({ keepHistory: value }),
  })
  const anonymousStats = computed({
    get: () => settings.value.anonymousStats,
    set: (value) => saveSettings({ anonymousStats: value }),
  })
  const reminderTime = computed({
    get: () => settings.value.reminderTime,
    set: (value) => saveSettings({ reminderTime: value }),
  })

  function isFavorite(id) {
    return topics.value.some((item) => item.id === Number(id) && item.favorite)
  }

  function findTopic(id) {
    return topics.value.find((item) => item.id === Number(id))
  }

  async function loadTopics() {
    const data = await api('/api/topics')
    categories.value = data.categories
    topics.value = data.topics
  }

  async function loadSettings() {
    const [goal, notice] = await Promise.all([api('/api/profile'), api('/api/profile/settings')])
    weeklyTarget.value = goal.weeklyTarget
    settings.value = {
      reminderOn: notice.reminderOn,
      reportNoticeOn: notice.reportNoticeOn,
      goalNoticeOn: notice.goalNoticeOn,
      keepHistory: notice.keepHistory,
      anonymousStats: notice.anonymousStats,
      reminderTime: notice.reminderTime || '20:00',
    }
  }

  async function load() {
    await Promise.all([loadTopics(), loadSettings()])
  }

  async function toggleFavorite(id) {
    const data = await api(`/api/topics/${id}/favorite`, { method: 'POST' })
    topics.value = topics.value.map((item) =>
      item.id === Number(id) ? { ...item, favorite: data.favorite } : item,
    )
  }

  async function setWeeklyTarget(value) {
    const next = Math.min(7, Math.max(1, value))
    const data = await api('/api/profile/goal', {
      method: 'PUT',
      body: { targetCount: next },
    })
    weeklyTarget.value = data.targetCount
  }

  async function saveSettings(patch) {
    const previous = settings.value
    const next = { ...previous, ...patch }
    settings.value = next
    try {
      await api('/api/profile/settings', { method: 'PUT', body: next })
    } catch (error) {
      settings.value = previous
      throw error
    }
  }

  return {
    topics,
    categories,
    weeklyTarget,
    reminderOn,
    reportNoticeOn,
    goalNoticeOn,
    keepHistory,
    anonymousStats,
    reminderTime,
    isFavorite,
    findTopic,
    load,
    loadTopics,
    toggleFavorite,
    setWeeklyTarget,
    saveSettings,
  }
})
