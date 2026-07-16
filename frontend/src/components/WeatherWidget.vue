<script setup>
/**
 * 👤 队员A — 天气小部件（顶部栏）
 * 功能: 实时天气 + 时段感知DJ欢迎语
 * 后端: module-chat (:8081) 调用和风天气API
 */
import { ref } from 'vue'

// TODO: 队员A 实现天气API调用
const weather = ref({ icon: '☀️', temp: '28°', city: '北京' })
const greeting = ref('下午好 ☀️')
</script>

<template>
  <div class="weather">
    <span class="greeting">{{ greeting }}</span>
    <span class="info">{{ weather.icon }} {{ weather.temp }} {{ weather.city }}</span>
  </div>
</template>

<style scoped>
.weather {
  display: flex; align-items: center; gap: 16px; font-size: 13px;
}
.greeting { color: var(--color-text-muted); }
.info { color: var(--color-accent); font-weight: 500; }
</style>
