<script setup>
/**
 * 👤 队员C — 每日推荐面板
 * 功能: 热门榜单 + 个性化推荐 + 偏好标签
 * 后端: module-rec (:8083)  Redis + RabbitMQ
 */
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { recApi } from '../api/rec'
import musicApi from '../api/music'
import { getCurrentUserId } from '../api/user'

const userId = ref(getCurrentUserId())
const connected = ref(false)
const refreshing = ref(false)
const refreshMessage = ref('')

// 热门榜单
const hotSongs = ref([])
const hotLoading = ref(true)

// 今日推荐
const dailySongs = ref([])
const dailyLoading = ref(true)

// 偏好标签
const tags = ref([])
const tagsLoading = ref(true)

onMounted(async () => {
  await loadAll()
  window.addEventListener('dj-user-session-changed', handleUserSessionChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('dj-user-session-changed', handleUserSessionChanged)
})

async function handleUserSessionChanged() {
  userId.value = getCurrentUserId()
  await loadAll()
}

async function loadAll() {
  hotLoading.value = true
  dailyLoading.value = true
  tagsLoading.value = true
  refreshMessage.value = ''
  tags.value = []

  try { await recApi.hello(); connected.value = true } catch (e) { connected.value = false }

  try {
    const res = await recApi.hot()
    hotSongs.value = res.data || []
  } catch (e) { /* 后端没启动时保持空列表 */ }
  hotLoading.value = false

  try {
    const res = await recApi.daily(userId.value)
    dailySongs.value = res.data || []
  } catch (e) { /* 后端没启动时保持空列表 */ }
  dailyLoading.value = false

  if (connected.value) {
    try {
      const res = await recApi.preferences(userId.value)
      if (res.data && res.data.length > 0) tags.value = res.data
    } catch (e) { /* 后端出错，用默认值 */ }
  }
  if (tags.value.length === 0) tags.value = ['摇滚', '电子', '流行']
  tagsLoading.value = false
}

async function refreshDaily() {
  refreshing.value = true
  refreshMessage.value = ''
  try {
    const res = await recApi.refreshDaily(userId.value)
    dailySongs.value = res.data || []
    const [hotRes, tagRes] = await Promise.allSettled([
      recApi.hot(),
      recApi.preferences(userId.value)
    ])
    if (hotRes.status === 'fulfilled') hotSongs.value = hotRes.value.data || []
    if (tagRes.status === 'fulfilled' && tagRes.value.data?.length) tags.value = tagRes.value.data
    refreshMessage.value = dailySongs.value.length > 0 ? '已按最新行为刷新' : '暂无足够行为数据'
  } catch (e) {
    refreshMessage.value = '刷新失败，请检查推荐服务'
  } finally {
    refreshing.value = false
  }
}

async function playRecommended(song) {
  if (!song?.songId) return
  try {
    const res = await musicApi.songDetail(song.songId)
    const detail = res.data?.data
    if (detail) {
      window.dispatchEvent(new CustomEvent('dj-play-song', { detail }))
    }
  } catch (e) {
    window.dispatchEvent(new CustomEvent('dj-play-song', {
      detail: {
        id: song.songId,
        title: song.title,
        artist: song.artist,
        source: 'LOCAL'
      }
    }))
  }
}
</script>

<template>
  <div class="panel rec-panel">
    <div class="panel-title">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      发现 & 推荐
      <button class="refresh-action" :disabled="refreshing" title="按最新播放和收藏行为刷新推荐" @click="refreshDaily">
        {{ refreshing ? '刷新中' : '刷新' }}
      </button>
    </div>

    <!-- 热门榜单 -->
    <div class="section">
      <div class="section-title">热门榜单</div>
      <div v-if="hotLoading" class="loading-text">加载中…</div>
      <div v-else-if="hotSongs.length === 0" class="loading-text">暂无榜单数据</div>
      <div v-else class="rank-list">
        <button v-for="s in hotSongs" :key="s.songId" class="rank-item" @click="playRecommended(s)">
          <span class="rank-badge">{{ s.rank }}</span>
          <div class="song-copy">
            <div class="song-name">{{ s.title || '歌曲#' + s.songId }}</div>
            <div class="song-artist">{{ s.artist || '—' }}</div>
          </div>
          <span class="hot-score">{{ s.score }}</span>
        </button>
      </div>
    </div>

    <!-- 今日推荐 -->
    <div class="section">
      <div class="section-title">今日推荐</div>
      <div v-if="refreshMessage" class="refresh-message">{{ refreshMessage }}</div>
      <div v-if="dailyLoading" class="loading-text">加载中…</div>
      <div v-else-if="dailySongs.length === 0" class="loading-text">暂无推荐数据</div>
      <div v-else class="recommend-list">
        <button v-for="s in dailySongs" :key="s.songId" class="rec-item" @click="playRecommended(s)">
          <div class="song-copy">
            <div class="song-name">{{ s.title || '歌曲#' + s.songId }}</div>
            <div class="song-artist">{{ s.artist || '—' }}</div>
          </div>
          <span v-if="s.reason" class="rec-reason">{{ s.reason }}</span>
        </button>
      </div>
    </div>

    <!-- 偏好标签 -->
    <div class="section">
      <div class="section-title">你的偏好</div>
      <div class="tags">
        <el-tag size="small" v-for="t in tags" :key="t" style="margin: 2px">{{ t }}</el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rec-panel { overflow-y: auto; display: flex; flex-direction: column; gap: var(--gap-md); }

.section { }
.section-title { font-size: 13px; color: var(--color-text-muted); margin-bottom: 8px; }
.loading-text { font-size: 12px; color: var(--color-text-muted); padding: 4px 0; }
.refresh-action {
  margin-left: auto;
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-muted);
  border-radius: var(--radius-sm);
  padding: 3px 8px;
  font-size: 11px;
  cursor: pointer;
}
.refresh-action:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-text); }
.refresh-action:disabled { opacity: 0.5; cursor: not-allowed; }
.refresh-message { margin: -2px 0 6px; font-size: 11px; color: var(--color-text-muted); }

.rank-item {
  width: 100%;
  display: flex; align-items: center; gap: 10px;
  padding: 7px 8px; border: 0; border-radius: var(--radius-sm); cursor: pointer;
  background: transparent; color: inherit; text-align: left;
  transition: background 0.15s;
}
.rank-item:hover { background: var(--color-surface-hover); }
.rank-badge {
  min-width: 24px; height: 24px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 600;
}
.song-copy { flex: 1; min-width: 0; }
.song-name { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-artist { font-size: 11px; color: var(--color-text-muted); }
.hot-score { margin-left: auto; font-size: 11px; color: var(--color-primary); }

.rec-item {
  width: 100%;
  padding: 7px 8px; border: 0; border-radius: var(--radius-sm); font-size: 13px;
  display: flex; gap: 8px; align-items: center; cursor: pointer;
  background: transparent; color: inherit; text-align: left;
}
.rec-item:hover { background: var(--color-surface-hover); }
.rec-reason {
  max-width: 46%;
  font-size: 11px; color: var(--color-accent); margin-left: auto;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.tags { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
