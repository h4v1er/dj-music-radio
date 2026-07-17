<script setup>
/**
 * 👤 队员A — 天气小部件（顶部栏）
 * 功能: 实时天气 + 时段感知DJ欢迎语
 * 后端: module-chat (:8081) 调用和风天气API
 */
import { computed, onMounted, ref } from 'vue'
import { Loading, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { chatApi } from '../api/chat'

const DEFAULT_CITY = '北京'
const GEOLOCATION_TIMEOUT = 5000
const WEATHER_CITY_STORAGE_KEY = 'dj-weather-city'
const WEATHER_LOCATION_STORAGE_KEY = 'dj-weather-location'

const weather = ref({
  icon: '☀️',
  temp: '28°',
  city: DEFAULT_CITY,
  text: '晴',
  source: 'demo',
  obsTime: '',
  message: '未配置真实天气服务',
  feelsLike: '',
  windDir: '',
  windScale: '',
  windSpeed: '',
  humidity: '',
  precip: '',
  pressure: '',
  vis: '',
  cloud: '',
  dew: '',
  updateTime: '',
  fxLink: ''
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
      message: sourceMessage(location.source, res.data.message),
      feelsLike: res.data.feelsLike || '',
      windDir: res.data.windDir || '',
      windScale: res.data.windScale || '',
      windSpeed: res.data.windSpeed || '',
      humidity: res.data.humidity || '',
      precip: res.data.precip || '',
      pressure: res.data.pressure || '',
      vis: res.data.vis || '',
      cloud: res.data.cloud || '',
      dew: res.data.dew || '',
      updateTime: res.data.updateTime || '',
      fxLink: res.data.fxLink || ''
    }
    rememberWeatherLocation(weather.value.city, location)
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
      source: 'geolocation',
      latitude,
      longitude
    }
  } catch (e) {
    return {
      value: DEFAULT_CITY,
      source: 'default',
      latitude: null,
      longitude: null
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

function rememberWeatherLocation(city, location) {
  if (city) {
    localStorage.setItem(WEATHER_CITY_STORAGE_KEY, city)
  }
  if (location.source === 'geolocation') {
    localStorage.setItem(WEATHER_LOCATION_STORAGE_KEY, JSON.stringify({
      city,
      latitude: location.latitude || null,
      longitude: location.longitude || null,
      source: location.source
    }))
  } else {
    localStorage.removeItem(WEATHER_LOCATION_STORAGE_KEY)
  }
}

const detailRows = computed(() => [
  ['城市', weather.value.city],
  ['天气', weather.value.text],
  ['温度', weather.value.temp],
  ['体感', weather.value.feelsLike],
  ['湿度', withUnit(weather.value.humidity, '%')],
  ['风向', weather.value.windDir],
  ['风力', withUnit(weather.value.windScale, '级')],
  ['风速', withUnit(weather.value.windSpeed, 'km/h')],
  ['降水', withUnit(weather.value.precip, 'mm')],
  ['气压', withUnit(weather.value.pressure, 'hPa')],
  ['能见度', withUnit(weather.value.vis, 'km')],
  ['云量', withUnit(weather.value.cloud, '%')],
  ['露点', weather.value.dew ? `${weather.value.dew}°` : ''],
  ['观测时间', formatTime(weather.value.obsTime)],
  ['更新时间', formatTime(weather.value.updateTime)],
  ['数据来源', weather.value.source === 'real' ? '和风天气实时数据' : '演示数据']
].filter(([, value]) => value))

function withUnit(value, unit) {
  return value ? `${value}${unit}` : ''
}

function formatTime(value) {
  if (!value) return ''
  return value.replace('T', ' ')
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
    <div v-else class="weather-detail">
      <span class="info">
        {{ weather.icon }} {{ weather.temp }} {{ weather.city }} {{ weather.text }}
        <em :class="{ real: weather.source === 'real' }">
          {{ weather.source === 'real' ? '实时' : '演示数据' }}
        </em>
        <em v-if="locationSource === 'geolocation'" class="location">定位</em>
      </span>
      <div class="weather-popover">
        <div class="popover-title">{{ weather.city }} 天气详情</div>
        <div v-for="[label, value] in detailRows" :key="label" class="detail-row">
          <span>{{ label }}</span>
          <strong>{{ value }}</strong>
        </div>
        <div class="message">{{ weather.message }}</div>
      </div>
    </div>
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

.weather-detail {
  position: relative;
  display: inline-flex;
}

.weather-popover {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 20;
  width: 310px;
  max-height: min(520px, calc(100vh - 80px));
  overflow: auto;
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

.weather-detail:hover .weather-popover {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

.popover-title {
  margin-bottom: 8px;
  color: var(--color-text);
  font-weight: 700;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 5px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  color: var(--color-text-muted);
}

.detail-row strong {
  color: var(--color-text);
  font-weight: 600;
  text-align: right;
}

.message {
  margin-top: 8px;
  color: var(--color-text-dim);
  font-size: 12px;
  line-height: 1.5;
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
