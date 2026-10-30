<script setup>
import TabBar from './TabBar.vue'

defineProps({
  tabs: { type: Boolean, default: true },
  label: { type: String, default: '声析 VOXINSIGHT' },
})
</script>

<template>
  <main class="stage">
    <section class="device" :aria-label="label">
      <slot name="bar" />
      <div class="scroll">
        <slot />
      </div>
      <div v-if="$slots.dock" class="dock">
        <slot name="dock" />
      </div>
      <TabBar v-if="tabs" />
      <slot name="overlay" />
    </section>
  </main>
</template>
