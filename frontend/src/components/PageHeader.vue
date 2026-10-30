<script setup>
defineProps({
  title: { type: String, required: true },
  backTo: { type: String, default: '' },
})
</script>

<template>
  <header class="page-bar">
    <RouterLink v-if="backTo" class="icon-btn" :to="backTo" aria-label="返回">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M15 5 8 12l7 7" />
      </svg>
    </RouterLink>
    <span v-else class="icon-btn"></span>
    <h1>{{ title }}</h1>
    <span class="icon-btn"></span>
  </header>
</template>
