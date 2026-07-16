<script setup>
/**
 * 👤 队员B — 音乐播放器面板
 * 功能: 播放器控制 + 歌单列表 + 歌单导入 + 音乐搜索
 * 后端: module-music (:8082)  MySQL + RabbitMQ
 */
import { ref, onMounted } from 'vue'
import { musicApi } from '../api/music'

const connected = ref(false)
const playing = ref(false)

onMounted(async () => {
  try { await musicApi.hello(); connected.value = true } catch(e) {}
})
</script>

<template>
  <div class="panel music-panel">
    <div class="panel-title">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      音乐中心
    </div>

    <!-- 播放器核心 -->
    <div class="player">
      <div class="album-art">🎵</div>
      <div class="track-info">
        <div class="track-name">未选择歌曲</div>
        <div class="track-artist">—</div>
      </div>
      <div class="controls">
        <el-button circle size="small">⏮</el-button>
        <el-button circle type="danger" size="large" @click="playing = !playing">
          {{ playing ? '⏸' : '▶' }}
        </el-button>
        <el-button circle size="small">⏭</el-button>
      </div>
      <div class="progress-bar">
        <div class="progress-fill" style="width: 30%"></div>
      </div>
    </div>

    <!-- 歌单区 -->
    <div class="section">
      <div class="section-title">📋 我的歌单</div>
      <div class="playlist">
        <div class="song" v-for="s in ['我的收藏', '每日推荐歌单', '摇滚时光']" :key="s">
          <span>🎵</span> {{ s }}
        </div>
      </div>
      <el-button size="small" type="primary" plain style="width:100%;margin-top:8px">
        ➕ 导入歌单
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.music-panel {
  display: flex; flex-direction: column; overflow-y: auto; gap: var(--gap-md);
}

/* 播放器 */
.player { text-align: center; }
.album-art {
  width: 100px; height: 100px; border-radius: var(--radius-md);
  background: linear-gradient(135deg, #1a1a3e, #2a1a3e);
  display: flex; align-items: center; justify-content: center;
  font-size: 48px; margin: 12px auto;
}
.track-name { font-size: 15px; font-weight: 600; margin-top: 8px; }
.track-artist { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; }
.controls {
  display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 16px;
}
.progress-bar {
  height: 4px; background: var(--color-border); border-radius: 2px;
  margin-top: 16px; overflow: hidden;
}
.progress-fill {
  height: 100%; background: var(--color-primary); border-radius: 2px;
  transition: width 0.3s;
}

/* 歌单 */
.section { margin-top: 8px; }
.section-title { font-size: 13px; color: var(--color-text-muted); margin-bottom: 8px; }
.song {
  padding: 6px 10px; border-radius: var(--radius-sm); font-size: 13px;
  cursor: pointer; display: flex; align-items: center; gap: 8px;
  transition: background 0.15s;
}
.song:hover { background: var(--color-surface-hover); }
</style>
