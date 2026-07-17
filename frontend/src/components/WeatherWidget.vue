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
const GEOLOCATION_TIMEOUT = 5000

const weather = ref({
  icon: '☀️',
  temp: '28°',
  city: DEFAULT_CITY,
  text: '晴',
  source: 'demo',
  obsTime: '',
  message: '未配置真实天气服务'
})
const greeting = ref('下午好，想听点什么？')
const loading = ref(false)
const failed = ref(false)
const locationSource = ref('default')

onMounted(() => {
  loadWeather()
})

async function loadWeather() {
  loading.value = true
  failed.value = false
  try {
    const location = await resolveWeatherLocation()
    const res = await chatApi.weather(location.value)
    weather.value = {
      icon: res.data.icon || '🌡️',
      temp: res.data.temp || '--°',
      city: res.data.city || DEFAULT_CITY,
      text: res.data.text || '未知',
      source: res.data.source || 'demo',
      obsTime: res.data.obsTime || '',
      message: sourceMessage(location.source, res.data.message)
    }
    locationSource.value = location.source
    greeting.value = res.data.greeting || '想听点什么？'
  } catch (e) {
    failed.value = true
    greeting.value = '天气服务待连接'
  } finally {
    loading.value = false
  }
}

async function resolveWeatherLocation() {
  try {
    const position = await currentPosition()
    const { latitude, longitude } = position.coords
    return {
      value: `${longitude.toFixed(4)},${latitude.toFixed(4)}`,
      source: 'geolocation'
    }
  } catch (e) {
    return {
      value: DEFAULT_CITY,
      source: 'default'
    }
  }
}

function currentPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('geolocation unsupported'))
      return
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: false,
      timeout: GEOLOCATION_TIMEOUT,
      maximumAge: 10 * 60 * 1000
    })
  })
}

function sourceMessage(source, message) {
  const prefix = source === 'geolocation' ? '浏览器定位城市' : '默认城市'
  return message ? `${prefix}；${message}` : prefix
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
    <span v-else class="info" :title="weather.message">
      {{ weather.icon }} {{ weather.temp }} {{ weather.city }} {{ weather.text }}
      <em :class="{ real: weather.source === 'real' }">
        {{ weather.source === 'real' ? '实时' : '演示数据' }}
      </em>
      <em v-if="locationSource === 'geolocation'" class="location">定位</em>
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

.info em.real {
  color: var(--color-accent);
}

.info em.location {
  color: var(--color-text-muted);
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
