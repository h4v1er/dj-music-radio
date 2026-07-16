<script setup>
/**
 * 歌曲情绪画像面板
 * 显示效价、唤醒度、能量条、情绪标签等
 */
import { ref, watch } from 'vue'
import api from '../../api/music'
import EmotionTag from './EmotionTag.vue'

const props = defineProps({
  songId: { type: Number, default: null },
  song: { type: Object, default: null }
})

const emotion = ref(null)
const loading = ref(false)
const analyzing = ref(false)

async function loadEmotion() {
  const id = props.songId
  if (!id) { emotion.value = null; return }
  loading.value = true
  try {
    const res = await api.getSongEmotion(id)
    emotion.value = res.data?.data || null
  } catch (e) {
    emotion.value = null
  } finally {
    loading.value = false
  }
}

watch(() => props.songId, loadEmotion, { immediate: true })

// 当 song prop 变化时也重新检查（可能在播放后拿到了歌词）
watch(() => props.song, (newSong) => {
  if (newSong && newSong.id === props.songId && emotion.value && !emotion.value.analyzed) {
    loadEmotion()
  }
})

async function triggerAnalyze() {
  if (!props.songId) return
  analyzing.value = true
  try {
    const res = await api.analyzeSongEmotion(props.songId)
    const data = res.data?.data
    if (data && data.analyzed) {
      // 刷新显示
      await loadEmotion()
      // 更新 song 上的 emotionTags
      if (props.song) {
        props.song.emotionTags = data.emotionTags || ''
      }
    }
  } catch (e) {
    console.error('情绪分析失败', e)
  } finally {
    analyzing.value = false
  }
}

// 判断是否可以手动触发分析（有歌词但未分析）
const canAnalyze = () => {
  if (!props.song) return false
  return props.song.lyric && (!emotion.value || !emotion.value.analyzed)
}

function pctStyle(val, max) {
  const pct = Math.max(0, Math.min(100, (val / max) * 100))
  return { width: pct + '%' }
}

function valenceColor(val) {
  if (val > 30) return 'var(--color-success)'
  if (val < -30) return 'var(--color-primary)'
  return 'var(--color-accent)'
}
</script>

<template>
  <div class="emotion-info">
    <div v-if="loading" class="loading-hint">分析中...</div>
    <template v-else-if="emotion && emotion.analyzed">
      <!-- 情绪标签 -->
      <div class="emotion-tags-row">
        <EmotionTag :emotion="emotion.primaryEmotion" size="md" />
        <EmotionTag v-if="emotion.secondaryEmotion" :emotion="emotion.secondaryEmotion" />
      </div>

      <!-- 效价 + 唤醒度 双条 -->
      <div class="bars">
        <div class="bar-row">
          <span class="bar-label">😊 效价</span>
          <span class="bar-val" :style="{ color: valenceColor(emotion.valence) }">{{ emotion.valenceLabel }}</span>
        </div>
        <div class="bar-track">
          <div class="bar-fill" :style="{ ...pctStyle(emotion.valence + 100, 200), background: valenceColor(emotion.valence) }"></div>
        </div>

        <div class="bar-row">
          <span class="bar-label">⚡ 能量</span>
          <span class="bar-val">{{ emotion.energyLabel }}</span>
        </div>
        <div class="bar-track">
          <div class="bar-fill energy-fill" :style="pctStyle(emotion.arousal, 100)"></div>
        </div>
      </div>

      <!-- 主题 & 场景 -->
      <div v-if="emotion.lyricTheme" class="meta-row">
        <span class="meta-label">📝 主题</span>
        <span>{{ emotion.lyricTheme }}</span>
      </div>
      <div v-if="emotion.suitableScenes?.length" class="meta-row">
        <span class="meta-label">🎯 场景</span>
        <span>{{ emotion.suitableScenes.join(' · ') }}</span>
      </div>
    </template>
    <div v-else class="empty-state">
      <p class="empty-hint">该歌曲暂无情绪数据</p>
      <button v-if="canAnalyze()" class="analyze-btn" :disabled="analyzing" @click="triggerAnalyze">
        {{ analyzing ? '⏳ 分析中...' : '🔬 分析情绪' }}
      </button>
      <p v-else-if="!props.song?.lyric" class="empty-tip">播放歌曲后将自动获取歌词并支持分析</p>
    </div>
  </div>
</template>

<style scoped>
.emotion-info { padding: 4px 0; }
.loading-hint, .empty-hint { font-size: 12px; color: var(--color-text-muted); text-align: center; padding: 8px; }
.empty-state { text-align: center; padding: 4px 0; }
.empty-tip { font-size: 11px; color: var(--color-text-dim); margin-top: 4px; }
.analyze-btn {
  margin-top: 8px; padding: 4px 16px; border: 1px solid var(--color-info);
  border-radius: var(--radius-sm); background: none; color: var(--color-info);
  font-size: 12px; cursor: pointer; transition: all 0.15s;
}
.analyze-btn:hover:not(:disabled) { background: var(--color-info); color: #fff; }
.analyze-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.emotion-tags-row { display: flex; gap: 6px; margin-bottom: 10px; flex-wrap: wrap; justify-content: center; }

.bars { margin: 8px 0; }
.bar-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
.bar-label, .bar-val { font-size: 11px; color: var(--color-text-muted); }
.bar-track {
  height: 4px; background: var(--color-border); border-radius: 2px;
  margin-bottom: 8px; overflow: hidden;
}
.bar-fill { height: 100%; border-radius: 2px; transition: width 0.3s; background: var(--color-info); }
.energy-fill { background: linear-gradient(90deg, var(--color-info), var(--color-accent), var(--color-primary)); }

.meta-row { display: flex; gap: 6px; font-size: 12px; margin-top: 4px; color: var(--color-text-muted); }
.meta-label { flex-shrink: 0; }
</style>
