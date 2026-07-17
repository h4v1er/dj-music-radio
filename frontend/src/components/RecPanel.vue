<script setup>
/**
 * 👤 队员C — 每日推荐面板
 * 功能: 热门榜单 + 个性化推荐 + 偏好标签
 * 后端: module-rec (:8083)  Redis + RabbitMQ
 */
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { recApi } from '../api/rec'
import { getCurrentUserId } from '../api/user'

const userId = ref(getCurrentUserId())
const connected = ref(false)

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
</script>

<template>
  <div class="panel rec-panel">
    <div class="panel-title">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      发现 & 推荐
    </div>

    <!-- 热门榜单 -->
    <div class="section">
      <div class="section-title">🔥 热门榜单</div>
      <div v-if="hotLoading" class="loading-text">加载中…</div>
      <div v-else-if="hotSongs.length === 0" class="loading-text">暂无榜单数据</div>
      <div v-else class="rank-list">
        <div v-for="s in hotSongs" :key="s.songId" class="rank-item">
          <span class="rank-badge">{{ s.rank === 1 ? '🥇' : s.rank === 2 ? '🥈' : s.rank === 3 ? '🥉' : '🎵' + s.rank }}</span>
          <div>
            <div class="song-name">{{ s.title || '歌曲#' + s.songId }}</div>
            <div class="song-artist">{{ s.artist || '—' }}</div>
          </div>
          <span class="hot-score">{{ s.score }}🔥</span>
        </div>
      </div>
    </div>

    <!-- 今日推荐 -->
    <div class="section">
      <div class="section-title">🎯 今日为你推荐</div>
      <div v-if="dailyLoading" class="loading-text">加载中…</div>
      <div v-else-if="dailySongs.length === 0" class="loading-text">暂无推荐数据</div>
      <div v-else class="recommend-list">
        <div v-for="s in dailySongs" :key="s.songId" class="rec-item">
          <span>🎵</span>
          <div>
            <div class="song-name">{{ s.title || '歌曲#' + s.songId }}</div>
            <div class="song-artist">{{ s.artist || '—' }}</div>
          </div>
          <span v-if="s.reason" class="rec-reason">{{ s.reason }}</span>
        </div>
      </div>
    </div>

    <!-- 偏好标签 -->
    <div class="section">
      <div class="section-title">🏷️ 你的偏好</div>
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

.rank-item {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 8px; border-radius: var(--radius-sm); cursor: pointer;
  transition: background 0.15s;
}
.rank-item:hover { background: var(--color-surface-hover); }
.rank-badge { font-size: 14px; min-width: 24px; }
.song-name { font-size: 13px; }
.song-artist { font-size: 11px; color: var(--color-text-muted); }
.hot-score { margin-left: auto; font-size: 11px; color: var(--color-primary); }

.rec-item {
  padding: 6px 10px; border-radius: var(--radius-sm); font-size: 13px;
  display: flex; gap: 8px; align-items: center; cursor: pointer; flex-wrap: wrap;
}
.rec-item:hover { background: var(--color-surface-hover); }
.rec-reason { font-size: 11px; color: var(--color-accent); margin-left: auto; }

.tags { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
