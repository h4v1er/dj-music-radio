<script setup>
/**
 * 👤 队员C — 每日推荐面板
 * 功能: 热门榜单 + 个性化推荐 + 定时推送 + 数据可视化
 * 后端: module-rec (:8083)  Redis + RabbitMQ
 */
import { ref, onMounted } from 'vue'
import { recApi } from '../api'

const connected = ref(false)
const hotSongs = ref([
  { rank: '🔥1', name: '加载中...', artist: '' },
  { rank: '🔥2', name: '加载中...', artist: '' },
  { rank: '🔥3', name: '加载中...', artist: '' },
])

onMounted(async () => {
  try { await recApi.hello(); connected.value = true } catch(e) {}
  // TODO: 从 Redis 加载热门榜单数据
})
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
      <div class="rank-list">
        <div v-for="(s, i) in hotSongs" :key="i" class="rank-item">
          <span class="rank-badge">{{ s.rank }}</span>
          <div>
            <div class="song-name">{{ s.name }}</div>
            <div class="song-artist">{{ s.artist || '—' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 今日推荐 -->
    <div class="section">
      <div class="section-title">🎯 今日为你推荐</div>
      <div class="recommend-list">
        <div class="rec-item" v-for="i in 3" :key="i">
          <span>🎵</span> 推荐歌曲 {{ i }}
        </div>
      </div>
    </div>

    <!-- 偏好标签 -->
    <div class="section">
      <div class="section-title">🏷️ 你的偏好</div>
      <div class="tags">
        <el-tag size="small" v-for="t in ['摇滚','电子','流行']" :key="t"
                style="margin: 2px">{{ t }}</el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rec-panel { overflow-y: auto; display: flex; flex-direction: column; gap: var(--gap-md); }

.section { }
.section-title { font-size: 13px; color: var(--color-text-muted); margin-bottom: 8px; }

.rank-item {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 8px; border-radius: var(--radius-sm); cursor: pointer;
  transition: background 0.15s;
}
.rank-item:hover { background: var(--color-surface-hover); }
.rank-badge { font-size: 13px; min-width: 24px; }
.song-name { font-size: 13px; }
.song-artist { font-size: 11px; color: var(--color-text-muted); }

.rec-item {
  padding: 6px 10px; border-radius: var(--radius-sm); font-size: 13px;
  display: flex; gap: 8px; align-items: center; cursor: pointer;
}
.rec-item:hover { background: var(--color-surface-hover); }

.tags { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
