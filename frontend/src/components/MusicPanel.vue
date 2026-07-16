<script setup>
/**
 * 👤 队员B — 音乐播放器面板（主组件）
 * 功能: 播放器 + 歌单管理 + 搜索 + 收藏 + 历史 + 导入
 * 后端: module-music (:8082)  MySQL + RabbitMQ
 */
import { ref, computed, onMounted } from 'vue'
import api, { getCoverUrl } from '../api/music'
import PlayerCore from './music/PlayerCore.vue'
import SongList from './music/SongList.vue'
import PlaylistPanel from './music/PlaylistPanel.vue'
import LyricsPanel from './music/LyricsPanel.vue'
import ImportDialog from './music/ImportDialog.vue'

// ── 连接状态 ──
const connected = ref(false)

// ── 播放状态 ──
const currentSong = ref(null)
const playQueue = ref([])
const playMode = ref('order')   // order | shuffle | repeat
const showLyrics = ref(false)

// ── UI 状态 ──
const activeTab = ref('all')    // all | playlists | favorites | history
const currentPlaylistId = ref(null)
const dragMode = ref(false)

// ── 数据 ──
const allSongs = ref([])
const playlistSongs = ref([])
const favoriteSongs = ref([])
const historySongs = ref([])
const lyricsTime = ref(0)

// ── 网易云 ──
const neteaseKw = ref('')
const neteaseResults = ref([])
const neteaseLoading = ref(false)
const neteaseCoverErrors = ref({})

// ── 子组件引用 ──
const importDialog = ref(null)

// ── 当前显示的歌曲列表 ──
const displaySongs = computed(() => {
  switch (activeTab.value) {
    case 'favorites': return favoriteSongs.value
    case 'history': return historySongs.value
    case 'playlists': return playlistSongs.value
    default: return allSongs.value
  }
})

const tabLabel = computed(() => {
  const labels = { all: '全部歌曲', playlists: '歌单歌曲', favorites: '我的收藏', history: '播放历史', netease: '☁️ 网易云' }
  return labels[activeTab.value] || '全部歌曲'
})

// ── 播放逻辑 ──
function playSong(song) {
  currentSong.value = song
  // 记录播放历史
  api.recordPlay(song.id).catch(() => {})
  // 检查收藏状态
  checkFavStatus(song)
  // 如果没在队列中，加入队列
  if (!playQueue.value.find(s => s.id === song.id)) {
    playQueue.value.push(song)
  }
}

function playNext() {
  if (playQueue.value.length === 0) return
  const idx = playQueue.value.findIndex(s => s.id === currentSong.value?.id)
  let nextIdx
  if (playMode.value === 'shuffle') {
    nextIdx = Math.floor(Math.random() * playQueue.value.length)
  } else if (idx >= 0 && idx < playQueue.value.length - 1) {
    nextIdx = idx + 1
  } else if (playMode.value === 'repeat') {
    nextIdx = idx // 单曲循环
  } else {
    nextIdx = 0 // 循环到开头
  }
  playSong(playQueue.value[nextIdx])
}

function playPrev() {
  if (playQueue.value.length === 0) return
  const idx = playQueue.value.findIndex(s => s.id === currentSong.value?.id)
  if (idx > 0) {
    playSong(playQueue.value[idx - 1])
  } else {
    playSong(playQueue.value[playQueue.value.length - 1])
  }
}

function addToQueue(song) {
  if (!playQueue.value.find(s => s.id === song.id)) {
    playQueue.value.push(song)
  }
}

// ── 收藏 ──
async function toggleFavorite(song) {
  try {
    const res = await api.checkFavorite(song.id)
    if (res.data?.data?.favorited) {
      await api.removeFavorite(song.id)
      song._favorited = false
    } else {
      await api.addFavorite(song.id)
      song._favorited = true
    }
    if (activeTab.value === 'favorites') await loadFavorites()
  } catch (e) { console.error('收藏操作失败', e) }
}

async function checkFavStatus(song) {
  try {
    const res = await api.checkFavorite(song.id)
    song._favorited = res.data?.data?.favorited || false
  } catch (e) { /* ignore */ }
}

// ── 歌单操作 ──
async function selectPlaylist(pl) {
  currentPlaylistId.value = pl.id
  activeTab.value = 'playlists'
  try {
    const res = await api.playlistSongs(pl.id)
    playlistSongs.value = res.data?.data || []
  } catch (e) { console.error('加载歌单歌曲失败', e) }
}

function onPlaylistDelete(plId) {
  if (currentPlaylistId.value === plId) {
    currentPlaylistId.value = null
    activeTab.value = 'all'
    playlistSongs.value = []
  }
}

// ── 拖拽排序 ──
async function onReorder(fromIdx, toIdx) {
  const songs = [...displaySongs.value]
  const [moved] = songs.splice(fromIdx, 1)
  songs.splice(toIdx, 0, moved)
  // 更新列表
  if (activeTab.value === 'playlists') playlistSongs.value = songs
  // 提交排序到后端
  if (currentPlaylistId.value) {
    try {
      await api.sortPlaylist(currentPlaylistId.value, songs.map(s => s.id))
    } catch (e) { console.error('排序失败', e) }
  }
}

// ── 数据加载 ──
async function loadSongs() {
  try {
    const res = await api.songList({ page: 1, size: 100 })
    allSongs.value = res.data?.data?.records || []
  } catch (e) { console.error('加载歌曲失败', e) }
}

async function loadFavorites() {
  try {
    const res = await api.favoriteList()
    favoriteSongs.value = res.data?.data || []
    favoriteSongs.value.forEach(s => s._favorited = true)
  } catch (e) { console.error('加载收藏失败', e) }
}

async function loadHistory() {
  try {
    const res = await api.historyList()
    historySongs.value = res.data?.data || []
  } catch (e) { console.error('加载历史失败', e) }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'favorites') loadFavorites()
  if (tab === 'history') loadHistory()
  if (tab === 'all') loadSongs()
  dragMode.value = false
}

function onTimeUpdate(t) {
  lyricsTime.value = t
}

// ── 网易云搜索 ──
let neteaseTimer = null
async function searchNetease() {
  const kw = neteaseKw.value.trim()
  if (!kw) { neteaseResults.value = []; return }
  neteaseLoading.value = true
  clearTimeout(neteaseTimer)
  neteaseTimer = setTimeout(async () => {
    try {
      const res = await api.neteaseSearch(kw, 20)
      const songs = res.data?.data?.songs || []
      neteaseResults.value = songs.map(s => {
        // 网易云封面 URL 强制升级为 HTTPS
        let rawUrl = (s.al || {}).picUrl || ''
        if (rawUrl && rawUrl.startsWith('http://')) {
          rawUrl = rawUrl.replace('http://', 'https://')
        }
        return {
          id: s.id,
          title: s.name,
          artist: (s.ar || []).map(a => a.name).join('/'),
          album: (s.al || {}).name || '',
          duration: Math.floor((s.dt || 0) / 1000),
          genre: '网易云',
          coverUrl: rawUrl,
          _netease: true
        }
      })
    } catch (e) { console.error('搜索失败', e) }
    neteaseLoading.value = false
  }, 400)
}

async function playNeteaseSong(song) {
  try {
    // 并行获取播放URL和歌词
    const [urlRes, lyricRes] = await Promise.all([
      api.neteaseUrl(song.id),
      api.neteaseLyric(song.id)
    ])
    // 设置播放URL
    const urlData = urlRes.data?.data
    if (urlData && urlData.length > 0 && urlData[0].url) {
      song.filePath = urlData[0].url
    }
    // 设置歌词
    const lyricData = lyricRes.data?.data
    if (lyricData?.lrc?.lyric) {
      song.lyric = lyricData.lrc.lyric
    }
  } catch (e) { console.error('获取播放数据失败', e) }
  currentSong.value = song
  playQueue.value.push(song)
}

// ── 初始化 ──
onMounted(async () => {
  try { await api.hello(); connected.value = true } catch (e) { /* ignore */ }
  await loadSongs()
})
</script>

<template>
  <div class="panel music-panel">
    <!-- 标题 -->
    <div class="panel-title">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      音乐中心
      <span class="title-actions">
        <button class="mini-btn" :class="{ active: showLyrics }" title="显示歌词" @click="showLyrics = !showLyrics">🎤</button>
        <button class="mini-btn" :class="{ active: dragMode }" title="拖拽排序" @click="dragMode = !dragMode">↕</button>
        <button class="mini-btn" title="导入歌单" @click="importDialog?.open()">📥</button>
      </span>
    </div>

    <!-- 播放器核心 -->
    <PlayerCore :currentSong="currentSong" :playQueue="playQueue"
                :playMode="playMode" @update:playMode="mode => playMode = mode"
                @play="() => {}" @pause="() => {}"
                @next="playNext" @prev="playPrev"
                @timeUpdate="onTimeUpdate" />

    <!-- 歌词面板 -->
    <LyricsPanel v-if="showLyrics" :lyrics="currentSong?.lyric || ''" :currentTime="lyricsTime" />

    <!-- 标签切换 -->
    <div class="tab-bar">
      <span v-for="tab in ['all', 'playlists', 'favorites', 'history', 'netease']" :key="tab"
            class="tab" :class="{ active: activeTab === tab }"
            @click="switchTab(tab)">
        {{ { all: '🎵 全部', playlists: '📋 歌单', favorites: '❤ 收藏', history: '🕐 历史', netease: '☁️ 网易云' }[tab] }}
      </span>
    </div>

    <!-- 歌单面板（选择歌单时显示） -->
    <PlaylistPanel v-if="activeTab === 'playlists'"
                   @select="selectPlaylist" @delete="onPlaylistDelete" @refresh="loadSongs" />

    <!-- 网易云搜索 -->
    <div v-if="activeTab === 'netease'" class="netease-panel">
      <div class="netease-search-bar">
        <input v-model="neteaseKw" type="text" placeholder="搜索网易云歌曲..."
               class="netease-input" @input="searchNetease" />
        <span v-if="neteaseLoading" class="loading-dot">⏳</span>
      </div>
      <div class="netease-results">
        <div v-if="neteaseResults.length === 0 && neteaseKw" class="empty-hint">
          {{ neteaseLoading ? '搜索中...' : '未找到歌曲，试试其他关键词' }}
        </div>
        <div v-for="song in neteaseResults" :key="song.id" class="netease-item"
             @click="playNeteaseSong(song)">
          <span class="netease-cover">
            <img v-if="getCoverUrl(song.coverUrl) && !neteaseCoverErrors[song.id]"
                 :src="getCoverUrl(song.coverUrl)" class="netease-thumb"
                 referrerpolicy="no-referrer"
                 @error="neteaseCoverErrors[song.id] = true"
                 @click.stop />
            <span v-else class="netease-cover-fallback">☁️</span>
          </span>
          <div class="song-meta">
            <div class="song-title">{{ song.title }}</div>
            <div class="song-artist">{{ song.artist }} · {{ song.album }}</div>
          </div>
          <span class="song-duration">{{ Math.floor(song.duration / 60) }}:{{ String(song.duration % 60).padStart(2, '0') }}</span>
        </div>
      </div>
      <div v-if="!neteaseKw" class="netease-hint">
        <p>☁️ 输入关键词搜索网易云音乐</p>
        <p style="font-size:11px;color:var(--color-text-dim)">搜到歌曲后点击即可在线播放</p>
      </div>
    </div>

    <!-- 歌曲列表 -->
    <SongList :songs="displaySongs" :currentSong="currentSong"
              :dragMode="dragMode && activeTab === 'playlists'"
              @play="playSong" @addToQueue="addToQueue"
              @favorite="toggleFavorite" @reorder="onReorder" />

    <!-- 播放队列 -->
    <div v-if="playQueue.length > 0" class="queue-bar">
      <span class="queue-label">📜 队列 ({{ playQueue.length }})</span>
      <span class="queue-mode">模式: {{ { order: '顺序', shuffle: '随机', repeat: '单曲循环' }[playMode] }}</span>
    </div>

    <!-- 导入对话框 -->
    <ImportDialog ref="importDialog" @imported="loadSongs" />
  </div>
</template>

<style scoped>
.music-panel {
  display: flex; flex-direction: column; overflow-y: auto; gap: 10px;
  max-height: calc(100vh - 88px);
}

/* 标题 */
.title-actions { margin-left: auto; display: flex; gap: 4px; }
.mini-btn {
  background: none; border: 1px solid var(--color-border);
  color: var(--color-text-muted); border-radius: var(--radius-sm);
  padding: 2px 6px; font-size: 11px; cursor: pointer; transition: all 0.15s;
}
.mini-btn:hover { border-color: var(--color-primary); color: var(--color-text); }
.mini-btn.active { background: var(--color-primary); border-color: var(--color-primary); color: #fff; }

/* 标签栏 */
.tab-bar { display: flex; gap: 6px; flex-wrap: wrap; }
.tab {
  font-size: 11px; padding: 4px 10px; border-radius: 10px;
  border: 1px solid var(--color-border); cursor: pointer;
  color: var(--color-text-muted); transition: all 0.15s;
}
.tab:hover { border-color: var(--color-primary); color: var(--color-text); }
.tab.active { background: var(--color-primary); border-color: var(--color-primary); color: #fff; }

/* 队列信息 */
.queue-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; border-radius: var(--radius-sm);
  background: var(--color-bg); font-size: 11px; color: var(--color-text-muted);
}
.queue-mode { font-size: 10px; }

/* 网易云 */
.netease-panel { }
.netease-search-bar { position: relative; margin-bottom: 8px; }
.netease-input {
  width: 100%; padding: 7px 10px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: var(--color-bg);
  color: var(--color-text); font-size: 12px; outline: none;
}
.netease-input:focus { border-color: var(--color-primary); }
.loading-dot { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); font-size: 14px; }
.netease-results { max-height: 280px; overflow-y: auto; }
.netease-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: var(--radius-sm); cursor: pointer;
  transition: background 0.15s; font-size: 12px;
}
.netease-item:hover { background: var(--color-surface-hover); }
.netease-cover {
  flex-shrink: 0; width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 4px; overflow: hidden;
  background: linear-gradient(135deg, #1a1a3e, #2a1a3e);
}
.netease-thumb {
  width: 36px; height: 36px; object-fit: cover; border-radius: 4px;
}
.netease-cover-fallback { font-size: 18px; }
.song-meta { flex: 1; min-width: 0; }
.song-title { font-size: 13px; font-weight: 500; }
.song-artist { font-size: 11px; color: var(--color-text-muted); }
.song-duration { font-size: 11px; color: var(--color-text-muted); flex-shrink: 0; }
.netease-hint { text-align: center; padding: 24px; color: var(--color-text-muted); font-size: 13px; }
</style>
