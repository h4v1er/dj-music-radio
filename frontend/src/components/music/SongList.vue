<script setup>
/**
 * 歌曲列表组件
 * 功能：歌曲列表展示、搜索筛选、流派过滤、点击播放、右键菜单、拖拽排序、情绪徽章
 */
import { ref, computed, watch, onMounted } from 'vue'
import api, { getCoverUrl } from '../../api/music'
import EmotionTag from './EmotionTag.vue'

const props = defineProps({
  songs: { type: Array, default: () => [] },
  currentSong: { type: Object, default: null },
  isFavorites: { type: Boolean, default: false },
  playlistId: { type: Number, default: null },
  dragMode: { type: Boolean, default: false }
})

const emit = defineEmits(['play', 'addToQueue', 'addToPlaylist', 'favorite', 'reorder'])

// ── 搜索 ──
const searchKw = ref('')
const searchResults = ref([])
const isSearching = ref(false)
const genres = ref([])
const selectedGenre = ref('')

// ── 右键菜单 ──
const contextMenu = ref({ show: false, x: 0, y: 0, song: null })

const displaySongs = computed(() => {
  return isSearching.value ? searchResults.value : props.songs
})

// ── 流派筛选 ──
const filteredSongs = computed(() => {
  if (!selectedGenre.value) return displaySongs.value
  return displaySongs.value.filter(s => s.genre === selectedGenre.value)
})

// ── 格式化 ──
function fmtDuration(s) {
  if (!s) return '--:--'
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}

// ── 封面错误跟踪 ──
const coverErrors = ref({})

function songCoverUrl(song) {
  if (!song?.coverUrl) return ''
  return getCoverUrl(song.coverUrl)
}

function onCoverError(songId) {
  coverErrors.value[songId] = true
}

// ── 搜索 ──
let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  const kw = searchKw.value.trim()
  if (!kw) {
    isSearching.value = false
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    try {
      const res = await api.search(kw, 1, 50)
      searchResults.value = res.data?.data?.records || []
      isSearching.value = true
    } catch (e) {
      console.error('搜索失败', e)
    }
  }, 300)
}

// ── 右键菜单 ──
function showContextMenu(e, song) {
  e.preventDefault()
  contextMenu.value = { show: true, x: e.clientX, y: e.clientY, song }
}
function hideContextMenu() {
  contextMenu.value.show = false
}
function addToQueue() {
  if (contextMenu.value.song) {
    emit('addToQueue', contextMenu.value.song)
  }
  hideContextMenu()
}
function addToPlaylist() {
  if (contextMenu.value.song) {
    emit('addToPlaylist', contextMenu.value.song)
  }
  hideContextMenu()
}
function toggleFavorite() {
  if (contextMenu.value.song) {
    emit('favorite', contextMenu.value.song)
  }
  hideContextMenu()
}
function playSongFromList(song) {
  emit('play', song)
}
function favoriteFromList(song) {
  emit('favorite', song)
}

// ── 拖拽 ──
function onDragStart(e, index) {
  if (!props.dragMode) return
  e.dataTransfer.setData('text/plain', String(index))
}
function onDrop(e, targetIndex) {
  if (!props.dragMode) return
  const fromIdx = parseInt(e.dataTransfer.getData('text/plain'))
  if (fromIdx === targetIndex) return
  emit('reorder', fromIdx, targetIndex)
}
function onDragOver(e) {
  if (props.dragMode) e.preventDefault()
}

onMounted(async () => {
  try {
    const res = await api.genres()
    genres.value = res.data?.data || []
  } catch (e) { /* ignore */ }
  document.addEventListener('click', hideContextMenu)
})
</script>

<template>
  <div class="song-list">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <input v-model="searchKw" type="text" placeholder="🔍 搜索歌曲、歌手、专辑..."
             class="search-input" @input="onSearchInput" />
      <span v-if="isSearching" class="search-badge">搜索结果</span>
    </div>

    <!-- 流派筛选 -->
    <div v-if="genres.length" class="genre-filter">
      <span class="genre-chip" :class="{ active: !selectedGenre }"
            @click="selectedGenre = ''">全部</span>
      <span v-for="g in genres" :key="g" class="genre-chip"
            :class="{ active: selectedGenre === g }"
            @click="selectedGenre = selectedGenre === g ? '' : g">{{ g }}</span>
    </div>

    <!-- 歌曲列表 -->
    <div class="song-items">
      <div v-if="filteredSongs.length === 0" class="empty-hint">
        {{ isSearching ? '未找到匹配歌曲' : '暂无歌曲' }}
      </div>
      <div v-for="(song, idx) in filteredSongs" :key="song.id"
           class="song-item"
           :class="{ active: currentSong?.id === song.id, 'drag-mode': dragMode }"
           :draggable="dragMode"
           @click="playSongFromList(song)"
           @contextmenu="showContextMenu($event, song)"
           @dragstart="onDragStart($event, idx)"
           @drop="onDrop($event, idx)"
           @dragover="onDragOver">
        <span class="song-num">{{ idx + 1 }}</span>
        <span class="song-cover">
          <img v-if="songCoverUrl(song) && !coverErrors[song.id]"
               :src="songCoverUrl(song)" class="cover-thumb"
               referrerpolicy="no-referrer"
               @error="onCoverError(song.id)" />
          <span v-else class="cover-emoji-sm">{{ { '摇滚': '🎸', '电子': '🎹', '流行': '🎤', '民谣': '🪕', '爵士': '🎷' }[song.genre] || '🎵' }}</span>
        </span>
        <div class="song-meta">
          <div class="song-title">{{ song.title }}</div>
          <div class="song-artist">{{ song.artist }} · {{ song.album || song.genre }}</div>
        </div>
        <span class="song-emotion-cell">
          <EmotionTag v-if="song.emotionTags" :emotion="song.emotionTags.split(',')[0]" size="sm" />
        </span>
        <span class="song-duration">{{ fmtDuration(song.duration) }}</span>
        <button class="fav-btn" :class="{ faved: song._favorited }"
                title="收藏" @click.stop="favoriteFromList(song)">❤</button>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div v-if="contextMenu.show" class="context-menu"
         :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
      <div class="ctx-item" @click="addToQueue">加入播放队列</div>
      <div class="ctx-item" @click="addToPlaylist">添加到歌单</div>
      <div class="ctx-item" @click="toggleFavorite">❤ 收藏/取消收藏</div>
      <div class="ctx-item" @click="playSongFromList(contextMenu.song); hideContextMenu()">▶ 播放</div>
    </div>
  </div>
</template>

<style scoped>
.song-list { display: flex; flex-direction: column; }

/* 搜索 */
.search-bar { position: relative; margin-bottom: 8px; }
.search-input {
  width: 100%; padding: 7px 10px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: var(--color-bg);
  color: var(--color-text); font-size: 12px; outline: none;
}
.search-input:focus { border-color: var(--color-primary); }
.search-badge {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: 10px; color: var(--color-primary); background: var(--color-primary-glow);
  padding: 1px 6px; border-radius: 3px;
}

/* 流派 */
.genre-filter {
  display: flex; gap: 6px; margin-bottom: 8px; flex-wrap: wrap;
}
.genre-chip {
  font-size: 11px; padding: 2px 10px; border-radius: 10px;
  border: 1px solid var(--color-border); cursor: pointer;
  color: var(--color-text-muted); transition: all 0.15s;
}
.genre-chip:hover { border-color: var(--color-primary); color: var(--color-text); }
.genre-chip.active {
  background: var(--color-primary); border-color: var(--color-primary); color: #fff;
}

/* 歌曲列表 */
.song-items { max-height: 320px; overflow-y: auto; }
.empty-hint {
  text-align: center; padding: 24px; font-size: 13px; color: var(--color-text-muted);
}
.song-item {
  display: grid;
  grid-template-columns: 24px 36px minmax(0, 1fr) 70px 46px 28px;
  align-items: center;
  gap: 8px;
  padding: 6px 8px; border-radius: var(--radius-sm); cursor: pointer;
  transition: background 0.15s; font-size: 12px;
}
.song-item:hover { background: var(--color-surface-hover); }
.song-item.active { background: rgba(233, 69, 96, 0.1); border-left: 2px solid var(--color-primary); }
.song-item.drag-mode { cursor: grab; }
.song-num { color: var(--color-text-muted); text-align: center; }
.song-cover { font-size: 18px; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; }
.cover-thumb {
  width: 32px; height: 32px; border-radius: 4px; object-fit: cover;
}
.cover-emoji-sm { font-size: 16px; }
.song-meta { flex: 1; min-width: 0; }
.song-title {
  font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.song-artist {
  font-size: 11px; color: var(--color-text-muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.song-emotion-cell {
  width: 70px;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.song-duration {
  width: 46px;
  font-size: 11px;
  color: var(--color-text-muted);
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.fav-btn {
  background: none; border: none; font-size: 13px; cursor: pointer;
  opacity: 0.3; transition: opacity 0.15s; padding: 2px;
}
.fav-btn:hover, .fav-btn.faved { opacity: 1; }

@media (max-width: 520px) {
  .song-item {
    grid-template-columns: 22px 34px minmax(0, 1fr) 42px 24px;
  }

  .song-emotion-cell {
    display: none;
  }
}

/* 右键菜单 */
.context-menu {
  position: fixed; z-index: 999;
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); box-shadow: var(--shadow-panel);
  min-width: 150px; padding: 4px 0;
}
.ctx-item {
  padding: 8px 14px; font-size: 12px; cursor: pointer; transition: background 0.15s;
}
.ctx-item:hover { background: var(--color-surface-hover); }
</style>
