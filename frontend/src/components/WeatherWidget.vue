<script setup>
/**
 * 👤 队员A — 天气小部件（顶部栏）
 * 功能: 实时天气 + 时段感知DJ欢迎语
 * 后端: module-chat (:8081) 调用和风天气API
 */
import { onMounted, ref } from 'vue'
import { Loading, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { chatApi } from '../api/chat'

const DEFAULT_CITY = '北京'

const weather = ref({ icon: '☀️', temp: '28°', city: DEFAULT_CITY, text: '晴', source: 'demo' })
const greeting = ref('下午好，想听点什么？')
const loading = ref(false)
const failed = ref(false)

onMounted(() => {
  loadWeather()
})

async function loadWeather() {
  loading.value = true
  failed.value = false
  try {
    const res = await chatApi.weather(DEFAULT_CITY)
    weather.value = {
      icon: res.data.icon || '🌡️',
      temp: res.data.temp || '--°',
      city: res.data.city || DEFAULT_CITY,
      text: res.data.text || '未知',
      source: res.data.source || 'demo'
    }
    greeting.value = res.data.greeting || '想听点什么？'
  } catch (e) {
    failed.value = true
    greeting.value = '天气服务待连接'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="weather">
    <span class="greeting">{{ greeting }}</span>
    <span v-if="loading" class="info muted">
      <el-icon class="spin"><Loading /></el-icon>
      加载中
    </span>
    <span v-else-if="failed" class="info failed">
      <el-icon><WarningFilled /></el-icon>
      天气不可用
    </span>
    <span v-else class="info">
      {{ weather.icon }} {{ weather.temp }} {{ weather.city }} {{ weather.text }}
      <em v-if="weather.source === 'demo'">演示</em>
    </span>
    <button class="refresh-btn" :disabled="loading" title="刷新天气" @click="loadWeather">
      <el-icon><Refresh /></el-icon>
    </button>
  </div>
</template>

<style scoped>
.weather {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  font-size: 13px;
}

.greeting {
  color: var(--color-text-muted);
  white-space: nowrap;
}

.info {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-accent);
  font-weight: 500;
  white-space: nowrap;
}

.info.muted {
  color: var(--color-text-muted);
}

.info.failed {
  color: var(--color-primary);
}

.info em {
  font-style: normal;
  font-size: 11px;
  color: var(--color-text-dim);
}

.refresh-btn {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  flex: 0 0 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  background: rgba(10, 10, 20, 0.45);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.refresh-btn:hover {
  color: var(--color-accent);
  border-color: var(--color-border-light);
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 720px) {
  .greeting {
    display: none;
  }
}
</style>
