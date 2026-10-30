<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { token } from '@/api/http'
import { useAppStore } from '@/stores/app'

const app = useAppStore()

onMounted(() => {
  if (token()) {
    app.load().catch(() => {})
  }
})
</script>

<template>
  <RouterView />
</template>
