<script setup>
/**
 * 👤 队员A — 智能对话 DJ 面板
 * 功能: AI对话 + WebSocket实时聊天 + DJ欢迎语(时段感知)
 * 后端: module-chat (:8081)
 */
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Connection, Headset, Promotion, User } from '@element-plus/icons-vue'
import { chatApi } from '../api/chat'
import { getCurrentUserId } from '../api/user'

const WEATHER_CITY_STORAGE_KEY = 'dj-weather-city'
const WEATHER_LOCATION_STORAGE_KEY = 'dj-weather-location'
const GEOLOCATION_TIMEOUT = 5000

defineProps({
  expanded: { type: Boolean, default: false }
})

const emit = defineEmits(['toggleExpand'])

const connected = ref(false)
const input = ref('')
const sending = ref(false)
const messages = ref([])
const messagesRef = ref(null)
let socket = null
let replyTimer = null

onMounted(async () => {
  await loadHistory()
  connectSocket()
  window.addEventListener('dj-user-session-changed', handleUserSessionChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('dj-user-session-changed', handleUserSessionChanged)
  clearReplyTimer()
  if (socket) {
    socket.close()
    socket = null
  }
})

async function send() {
  const content = input.value.trim()
  if (!content || sending.value) return

  messages.value.push({ role: 'user', text: content, time: '刚刚' })
  input.value = ''
  sending.value = true
  await scrollToBottom()

  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ type: 'message', userId: currentUserId(), content, context: currentChatContext() }))
    startReplyTimer()
    return
  }

  await sendByRest(content)
}

function connectSocket() {
  try {
    socket = chatApi.createSocket(currentUserId())
    socket.onopen = () => {
      connected.value = true
    }
    socket.onmessage = async (event) => {
      const data = JSON.parse(event.data)
      if (data.type === 'connected') {
        connected.value = true
        return
      }
      clearReplyTimer()
      if (data.type === 'tool_request') {
        await handleToolRequest(data)
        return
      }
      sending.value = false
      if (data.type === 'reply') {
        messages.value.push({
          role: 'dj',
          text: data.content,
          songs: normalizeSongItems(data.selectedSongs, data.songs || []),
          time: data.time || '刚刚'
        })
      } else if (data.type === 'error') {
        messages.value.push({
          role: 'dj',
          text: data.content || '刚才没有收到有效回复。',
          time: '刚刚'
        })
      }
      await scrollToBottom()
    }
    socket.onclose = () => {
      connected.value = false
      clearReplyTimer()
      sending.value = false
    }
    socket.onerror = () => {
      connected.value = false
    }
  } catch (e) {
    connected.value = false
  }
}

async function handleUserSessionChanged() {
  if (socket) {
    socket.close()
    socket = null
  }
  await loadHistory()
  connectSocket()
}

async function loadHistory() {
  try {
    await chatApi.hello()
    connected.value = true
    const res = await chatApi.history(currentUserId())
    messages.value = normalizeMessages(res.data)
    await scrollToBottom()
  } catch (e) {
    connected.value = false
    messages.value = [
      { role: 'dj', text: 'DJ 服务暂时连不上，请稍后再试。', time: '' }
    ]
  }
}

function pushRestReply(data) {
  const reply = data.reply
  const songs = data.songs || []
  messages.value.push({
    role: reply.role || 'dj',
    text: reply.text,
    songs: normalizeSongItems(data.selectedSongs, songs),
    time: reply.time || '刚刚'
  })
}

async function handleToolRequest(data) {
  try {
    const context = await executeClientTools(data.clientToolRequests || [])
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({
        type: 'message',
        userId: currentUserId(),
        content: data.content || input.value.trim(),
        context
      }))
      startReplyTimer()
      return
    }
    await sendByRest(data.content || input.value.trim(), context)
  } catch (e) {
    sending.value = false
    messages.value.push({
      role: 'dj',
      text: '我没能获取到浏览器定位。你可以允许定位权限，或者直接告诉我所在城市。',
      time: '刚刚'
    })
    await scrollToBottom()
  }
}

async function executeClientTools(requests) {
  const context = currentChatContext()
  for (const request of requests) {
    if (request.name === 'location.current') {
      context.location = await resolveCurrentLocation()
    }
  }
  return context
}

async function resolveCurrentLocation() {
  const position = await currentPosition()
  const { latitude, longitude } = position.coords
  const value = `${longitude.toFixed(4)},${latitude.toFixed(4)}`
  const res = await chatApi.weather(value)
  const location = {
    city: res.data.city || '',
    latitude,
    longitude,
    source: 'browser_geolocation'
  }
  rememberLocation(location)
  return location
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

function currentChatContext() {
  const stored = localStorage.getItem(WEATHER_LOCATION_STORAGE_KEY)
  if (stored) {
    try {
      const location = JSON.parse(stored)
      if (location && location.source !== 'default' && (location.city || (location.latitude && location.longitude))) {
        return { location }
      }
    } catch (e) {
      localStorage.removeItem(WEATHER_LOCATION_STORAGE_KEY)
    }
  }
  return {}
}

function rememberLocation(location) {
  if (location.city) {
    localStorage.setItem(WEATHER_CITY_STORAGE_KEY, location.city)
  }
  localStorage.setItem(WEATHER_LOCATION_STORAGE_KEY, JSON.stringify(location))
}

async function sendByRest(content, context) {
  try {
    const res = await chatApi.send({ userId: currentUserId(), content, context: context || currentChatContext() })
    if (Array.isArray(res.data.clientToolRequests) && res.data.clientToolRequests.length) {
      const nextContext = await executeClientTools(res.data.clientToolRequests)
      const nextRes = await chatApi.send({ userId: currentUserId(), content, context: nextContext })
      pushRestReply(nextRes.data)
      connected.value = true
      return
    }
    pushRestReply(res.data)
    connected.value = true
  } catch (e) {
    connected.value = false
    messages.value.push({
      role: 'dj',
      text: '刚才没有连上后端，我稍后再帮你推荐。',
      time: '刚刚'
    })
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function currentWeatherCity() {
  return localStorage.getItem(WEATHER_CITY_STORAGE_KEY) || ''
}

function currentUserId() {
  return getCurrentUserId()
}

function startReplyTimer() {
  clearReplyTimer()
  replyTimer = window.setTimeout(async () => {
    connected.value = false
    sending.value = false
    messages.value.push({
      role: 'dj',
      text: 'WebSocket 暂时没有响应，我稍后再帮你推荐。',
      time: '刚刚'
    })
    await scrollToBottom()
  }, 5000)
}

function clearReplyTimer() {
  if (replyTimer) {
    window.clearTimeout(replyTimer)
    replyTimer = null
  }
}

function normalizeMessages(list) {
  if (!Array.isArray(list) || list.length === 0) {
    return [{ role: 'dj', text: '下午好！我是你的 DJ 助手，想听点什么？', time: '' }]
  }
  return list.map(item => ({ ...item, songs: item.songs || [] }))
}

function normalizeSongItems(selectedSongs, fallbackSongs) {
  if (Array.isArray(selectedSongs) && selectedSongs.length) {
    return selectedSongs.map(song => ({
      ...song,
      label: songLabel(song),
      source: song.source || (song.netease ? 'NETEASE' : 'PROJECT')
    }))
  }
  if (!Array.isArray(fallbackSongs)) return []
  return fallbackSongs.map((song, index) => ({
    id: null,
    sourceId: '',
    title: String(song),
    artist: '',
    source: 'TEXT',
    playable: false,
    label: String(song),
    _key: `text-${index}-${song}`
  }))
}

function songLabel(song) {
  if (song.label) return song.label
  if (!song.artist) return song.title || '未知歌曲'
  return `${song.title} - ${song.artist}`
}

function playChatSong(song) {
  if (!song?.playable) return
  window.dispatchEvent(new CustomEvent('dj-play-song', {
    detail: normalizePlayableSong(song)
  }))
}

function normalizePlayableSong(song) {
  const netease = song.netease || song.source === 'NETEASE'
  const sourceId = song.sourceId || (netease ? song.id : '')
  return {
    id: song.id || song.songId || sourceId,
    sourceId,
    title: song.title,
    artist: song.artist,
    album: song.album || '',
    genre: song.genre || (netease ? '网易云' : ''),
    coverUrl: song.coverUrl || '',
    filePath: song.filePath || '',
    duration: normalizeDuration(song.duration),
    source: netease ? 'NETEASE' : song.source,
    _netease: netease
  }
}

function normalizeDuration(duration) {
  const value = Number(duration || 0)
  if (!Number.isFinite(value)) return 0
  return value > 10000 ? Math.floor(value / 1000) : value
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<template>
  <div class="panel chat-panel" :class="{ expanded }">
    <div class="chat-header">
      <div class="title-group">
        <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
        <div>
          <span class="panel-kicker">AI 对话 DJ</span>
          <strong>智能音乐助手</strong>
        </div>
      </div>
      <span :class="['status-pill', connected ? 'online' : 'offline']">
        <el-icon><Connection /></el-icon>
        {{ connected ? '实时' : '离线' }}
      </span>
      <button class="expand-btn" :title="expanded ? '还原布局' : '放大聊天'" @click="emit('toggleExpand')">
        {{ expanded ? '还原' : '放大' }}
      </button>
    </div>

    <!-- 消息区 -->
    <div ref="messagesRef" class="messages">
      <div v-for="(m, i) in messages" :key="i" :class="['msg-row', m.role]">
        <div class="avatar">
          <el-icon v-if="m.role === 'dj'"><Headset /></el-icon>
          <el-icon v-else><User /></el-icon>
        </div>
        <div class="bubble">
          <div class="msg-meta">
            <span>{{ m.role === 'dj' ? 'DJ' : '你' }}</span>
            <time v-if="m.time">{{ m.time }}</time>
          </div>
          <p>{{ m.text }}</p>
          <ol v-if="m.songs?.length" class="song-links">
            <li v-for="(song, songIndex) in m.songs" :key="song.sourceId || song.id || song._key || songIndex">
              <button
                class="song-link"
                :class="{ disabled: !song.playable }"
                :disabled="!song.playable"
                :title="song.playable ? '播放这首歌' : '这条结果暂时不能直接播放'"
                @click="playChatSong(song)"
              >
                <span class="song-link-title">{{ song.label || songLabel(song) }}</span>
                <span v-if="song.source && song.source !== 'TEXT'" class="song-link-source">{{ song.source === 'NETEASE' ? '网易云' : '本地' }}</span>
              </button>
            </li>
          </ol>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-row">
      <el-input
        v-model="input"
        type="textarea"
        resize="none"
        :autosize="{ minRows: 1, maxRows: 3 }"
        placeholder="输入想听的风格或心情"
        :disabled="sending"
        @keydown.enter.exact.prevent="send"
      />
      <button class="send-btn" :disabled="sending" title="发送" @click="send">
        <el-icon><Promotion /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  gap: 14px;
  padding: 18px;
  transition: padding 0.28s ease, box-shadow 0.28s ease;
}

.chat-panel.expanded {
  padding: 20px;
}

.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.title-group .dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex: 0 0 auto;
  box-shadow: 0 0 12px rgba(78, 204, 163, 0.45);
}

.title-group div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.panel-kicker {
  color: var(--color-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.title-group strong {
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.2;
  letter-spacing: 0;
}

.status-pill {
  height: 24px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  background: rgba(10, 10, 20, 0.72);
  font-size: 11px;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-pill.online {
  color: var(--color-success);
  border-color: rgba(78, 204, 163, 0.28);
}

.status-pill.offline {
  color: var(--color-text-dim);
}

.expand-btn {
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 11px;
  cursor: pointer;
  white-space: nowrap;
}

.expand-btn:hover {
  border-color: var(--color-info);
  color: var(--color-text);
  background: rgba(15, 155, 255, 0.08);
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 2px 2px 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex: 0 0 28px;
  color: var(--color-primary);
  background: rgba(233, 69, 96, 0.12);
  border: 1px solid rgba(233, 69, 96, 0.25);
}

.msg-row.user .avatar {
  color: var(--color-info);
  background: rgba(15, 155, 255, 0.12);
  border-color: rgba(15, 155, 255, 0.26);
}

.bubble {
  max-width: min(720px, calc(100% - 44px));
  padding: 10px 12px;
  border-radius: 12px 12px 12px 4px;
  background: rgba(233, 69, 96, 0.11);
  border: 1px solid rgba(233, 69, 96, 0.2);
  color: var(--color-text);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.16);
}

.msg-row.user .bubble {
  border-radius: 12px 12px 4px 12px;
  background: rgba(15, 155, 255, 0.13);
  border-color: rgba(15, 155, 255, 0.24);
}

.msg-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--color-text-muted);
  font-size: 11px;
  line-height: 1;
  margin-bottom: 6px;
}

.msg-meta span {
  font-weight: 700;
}

.msg-meta time {
  color: var(--color-text-dim);
  white-space: nowrap;
}

.bubble p {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.song-links {
  margin: 10px 0 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.song-links li {
  padding-left: 2px;
}

.song-link {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--color-text);
  background: rgba(255, 255, 255, 0.04);
  text-align: left;
  cursor: pointer;
}

.song-link:hover:not(:disabled) {
  border-color: rgba(15, 155, 255, 0.35);
  background: rgba(15, 155, 255, 0.1);
}

.song-link.disabled {
  cursor: default;
  opacity: 0.75;
}

.song-link-title {
  min-width: 0;
  overflow-wrap: anywhere;
}

.song-link-source {
  flex: 0 0 auto;
  color: var(--color-text-muted);
  font-size: 11px;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.input-row :deep(.el-textarea__inner) {
  min-height: 42px !important;
  padding: 10px 12px;
  border-radius: 10px;
  border-color: var(--color-border);
  background: rgba(10, 10, 20, 0.78);
  color: var(--color-text);
  line-height: 1.45;
  box-shadow: none;
}

.input-row :deep(.el-textarea__inner:focus) {
  border-color: var(--color-info);
  box-shadow: 0 0 0 2px rgba(15, 155, 255, 0.12);
}

.send-btn {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex: 0 0 42px;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary), #d83a32);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, opacity 0.2s;
}

.send-btn:hover {
  box-shadow: var(--shadow-glow);
  transform: translateY(-1px);
}

.send-btn .el-icon {
  font-size: 18px;
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

@media (max-width: 1000px) {
  .chat-panel {
    min-height: 520px;
  }
}
</style>
