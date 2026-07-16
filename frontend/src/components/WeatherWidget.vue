<script setup>
/**
 * 👤 队员A — 天气小部件（顶部栏）
 * 功能: 实时天气 + 时段感知DJ欢迎语
 * 后端: module-chat (:8081) 调用和风天气API
 */
import { onMounted, ref } from 'vue'
import { chatApi } from '../api/chat'

const weather = ref({ icon: '☀️', temp: '28°', city: '北京', text: '晴' })
const greeting = ref('下午好，想听点什么？')

onMounted(async () => {
  try {
    const res = await chatApi.weather('北京')
    weather.value = {
      icon: res.data.icon,
      temp: res.data.temp,
      city: res.data.city,
      text: res.data.text
    }
    greeting.value = res.data.greeting
  } catch (e) {
    greeting.value = 'DJ 天气服务待连接'
  }
})
</script>

<template>
  <div class="weather">
    <span class="greeting">{{ greeting }}</span>
    <span class="info">{{ weather.icon }} {{ weather.temp }} {{ weather.city }} {{ weather.text }}</span>
  </div>
</template>

<style scoped>
.weather {
  display: flex; align-items: center; gap: 16px; font-size: 13px;
}
.greeting { color: var(--color-text-muted); }
.info { color: var(--color-accent); font-weight: 500; }
</style>
