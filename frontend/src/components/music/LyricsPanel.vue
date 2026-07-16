<script setup>
/**
 * 歌词面板组件
 * 功能：LRC 歌词解析、实时同步高亮当前行、情绪标签
 */
import { ref, computed, watch } from 'vue'
import EmotionTag from './EmotionTag.vue'

const props = defineProps({
  lyrics: { type: String, default: '' },
  currentTime: { type: Number, default: 0 },
  emotionTags: { type: String, default: '' }  // 情绪标签（逗号分隔）
})

// ── LRC 解析 ──
const lines = computed(() => {
  if (!props.lyrics) return []
  const result = []
  const regex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/g
  let match
  while ((match = regex.exec(props.lyrics)) !== null) {
    const minutes = parseInt(match[1])
    const seconds = parseInt(match[2])
    const centis = match[3].length === 2 ? parseInt(match[3]) * 10 : parseInt(match[3])
    const time = minutes * 60 + seconds + centis / 1000
    const text = match[4].trim()
    if (text) result.push({ time, text })
  }
  return result
})

const activeIndex = ref(-1)

watch(() => props.currentTime, (t) => {
  if (!lines.value.length) { activeIndex.value = -1; return }
  // 找到当前时间对应的歌词行
  let idx = -1
  for (let i = 0; i < lines.value.length; i++) {
    if (lines.value[i].time <= t) idx = i
    else break
  }
  activeIndex.value = idx
})

// 滚动到当前行
watch(activeIndex, (idx) => {
  if (idx < 0) return
  const el = document.querySelector(`.lyric-line[data-idx="${idx}"]`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
})
</script>

<template>
  <div class="lyrics-panel">
    <div class="section-title">
      🎤 歌词
      <EmotionTag v-if="emotionTags" :emotion="emotionTags.split(',')[0]" size="sm" style="margin-left:6px" />
    </div>
    <div v-if="lines.length === 0" class="empty-hint">暂无歌词</div>
    <div v-else class="lyrics-scroll">
      <div v-for="(line, idx) in lines" :key="idx"
           class="lyric-line"
           :class="{ active: idx === activeIndex, upcoming: idx > activeIndex && idx <= activeIndex + 2 }"
           :data-idx="idx">
        {{ line.text }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.lyrics-panel { }

.section-title { font-size: 12px; color: var(--color-text-muted); margin-bottom: 8px; }
.empty-hint { text-align: center; padding: 16px; font-size: 13px; color: var(--color-text-muted); }

.lyrics-scroll {
  max-height: 200px; overflow-y: auto; text-align: center;
  mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%);
}
.lyric-line {
  padding: 4px 0; font-size: 13px; color: var(--color-text-muted);
  transition: all 0.3s; cursor: default;
}
.lyric-line.active {
  color: var(--color-primary); font-size: 15px; font-weight: 600;
  text-shadow: 0 0 8px var(--color-primary-glow);
}
.lyric-line.upcoming { color: var(--color-text); }
</style>
