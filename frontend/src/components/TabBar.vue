<script setup>
const items = [
  { name: 'home', to: '/', label: '首页' },
  { name: 'topics', to: '/topics', label: '题库' },
  { name: 'history', to: '/history', label: '历史' },
  { name: 'profile', to: '/profile', label: '我的' },
]
</script>

<template>
  <nav class="tabbar" aria-label="主导航">
    <RouterLink
      v-for="item in items"
      :key="item.name"
      :to="item.to"
      exact-active-class="active"
    >
      <svg v-if="item.name === 'home'" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1v-9.5Z" />
      </svg>
      <svg v-else-if="item.name === 'topics'" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M6 4.5h9.5A2.5 2.5 0 0 1 18 7v12.5H8.5A2.5 2.5 0 0 1 6 17V4.5Z" />
        <path d="M6 17a2.5 2.5 0 0 1 2.5-2.5H18" />
      </svg>
      <svg v-else-if="item.name === 'history'" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="8" />
        <path d="M12 8v4.5l3 2" />
      </svg>
      <svg v-else viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="8" r="3.2" />
        <path d="M5.5 19.2c1.2-2.6 3.4-4 6.5-4s5.3 1.4 6.5 4" />
      </svg>
      {{ item.label }}
    </RouterLink>
  </nav>
</template>
