<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Clock } from '@element-plus/icons-vue'

const now = ref(new Date())
let timer = null

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})

const timeText = computed(() => {
  return now.value.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
})

const dateText = computed(() => {
  return now.value.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  })
})

const detailRows = computed(() => [
  ['完整日期', now.value.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })],
  ['当前时间', now.value.toLocaleTimeString('zh-CN', { hour12: false })],
  ['时区', Intl.DateTimeFormat().resolvedOptions().timeZone || '本地时区']
])
</script>

<template>
  <div class="time-widget">
    <div class="time-main">
      <el-icon><Clock /></el-icon>
      <span class="clock">{{ timeText }}</span>
      <span class="date">{{ dateText }}</span>
    </div>
    <div class="time-popover">
      <div v-for="[label, value] in detailRows" :key="label" class="detail-row">
        <span>{{ label }}</span>
        <strong>{{ value }}</strong>
      </div>
    </div>
  </div>
</template>

<style scoped>
.time-widget {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-width: 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

.time-main {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.clock {
  color: var(--color-text);
  font-weight: 600;
}

.date {
  color: var(--color-text-muted);
}

.time-popover {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 20;
  min-width: 230px;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: rgba(18, 18, 32, 0.96);
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.35);
  opacity: 0;
  pointer-events: none;
  transform: translateY(-4px);
  transition: opacity 0.16s, transform 0.16s;
}

.time-widget:hover .time-popover {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 5px 0;
  color: var(--color-text-muted);
}

.detail-row strong {
  color: var(--color-text);
  font-weight: 600;
  text-align: right;
}
</style>
