<script setup>
/**
 * 播放器核心组件
 * 功能：封面展示、播放控制、进度条、音量调节、播放模式切换
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { getCoverUrl } from '../../api/music'

const props = defineProps({
  currentSong: { type: Object, default: null },
  playQueue: { type: Array, default: () => [] },
  playMode: { type: String, default: 'order' } // order | shuffle | repeat
})

const emit = defineEmits(['play', 'pause', 'next', 'prev', 'update:playMode', 'timeUpdate'])

// ── 状态 ──
const audio = ref(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(0.7)
const isMuted = ref(false)
const prevVolume = ref(0.7)
const coverError = ref(false)
const coverLoaded = ref(false)

// ── 计算属性 ──
const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const formattedCurrent = computed(() => formatTime(currentTime.value))
const formattedDuration = computed(() => formatTime(duration.value))

const modeIcon = computed(() => {
  const icons = { order: '🔁', shuffle: '🔀', repeat: '🔂' }
  return icons[props.playMode] || '🔁'
})

const modeLabel = computed(() => {
  const labels = { order: '顺序播放', shuffle: '随机播放', repeat: '单曲循环' }
  return labels[props.playMode] || '顺序播放'
})

const coverUrl = computed(() => {
  if (!props.currentSong?.coverUrl) return ''
  return getCoverUrl(props.currentSong.coverUrl)
})

const coverEmoji = computed(() => {
  if (!props.currentSong) return '🎵'
  const map = { '摇滚': '🎸', '电子': '🎹', '流行': '🎤', '民谣': '🪕', '爵士': '🎷' }
  return map[props.currentSong.genre] || '🎵'
})

// ── 音频事件 ──
function onTimeUpdate() {
  if (audio.value) {
    currentTime.value = audio.value.currentTime
    emit('timeUpdate', currentTime.value)
  }
}

function onLoaded() {
  if (audio.value) {
    duration.value = audio.value.duration || 0
    audio.value.volume = volume.value
  }
}

function onEnded() {
  emit('next')
}

// ── 播放控制 ──
function togglePlay() {
  if (!audio.value || !props.currentSong) return
  if (isPlaying.value) {
    audio.value.pause()
    isPlaying.value = false
    emit('pause')
  } else {
    audio.value.play().catch(() => {})
    isPlaying.value = true
    emit('play')
  }
}

function seekTo(e) {
  if (!audio.value) return
  const bar = e.currentTarget
  const rect = bar.getBoundingClientRect()
  const pct = (e.clientX - rect.left) / rect.width
  audio.value.currentTime = pct * duration.value
}

function changeVolume(e) {
  if (!audio.value) return
  volume.value = parseFloat(e.target.value)
  audio.value.volume = volume.value
  isMuted.value = false
}

function toggleMute() {
  if (!audio.value) return
  if (isMuted.value) {
    volume.value = prevVolume.value
    audio.value.volume = volume.value
    isMuted.value = false
  } else {
    prevVolume.value = volume.value
    volume.value = 0
    audio.value.volume = 0
    isMuted.value = true
  }
}

function cycleMode() {
  const modes = ['order', 'shuffle', 'repeat']
  const idx = modes.indexOf(props.playMode)
  const next = modes[(idx + 1) % modes.length]
  emit('update:playMode', next)
}

function formatTime(s) {
  if (!s || isNaN(s)) return '00:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

// ── 监听歌曲切换 ──
watch(() => props.currentSong?.id, (newId, oldId) => {
  // 重置封面状态
  coverError.value = false
  coverLoaded.value = false
  if (!props.currentSong || !audio.value) return
  // 同一首歌不重新加载
  if (newId === oldId && isPlaying.value) return
  if (props.currentSong.filePath) {
    // 先暂停当前播放
    audio.value.pause()
    // 设置新的音频源
    audio.value.src = props.currentSong.filePath
    isPlaying.value = false
    currentTime.value = 0
    // 直接播放（load 由浏览器自动处理）
    audio.value.play().then(() => {
      isPlaying.value = true
      emit('play')
    }).catch((err) => {
      console.warn('⚠️ 播放失败:', err.message)
      console.warn('💡 提示：点击播放按钮或双击歌曲来启动播放')
      isPlaying.value = false
    })
  }
})

onMounted(() => {
  audio.value = new Audio()
  audio.value.addEventListener('timeupdate', onTimeUpdate)
  audio.value.addEventListener('loadedmetadata', onLoaded)
  audio.value.addEventListener('ended', onEnded)
  audio.value.volume = volume.value
})

onUnmounted(() => {
  if (audio.value) {
    audio.value.pause()
    audio.value.removeEventListener('timeupdate', onTimeUpdate)
    audio.value.removeEventListener('loadedmetadata', onLoaded)
    audio.value.removeEventListener('ended', onEnded)
    audio.value = null
  }
})
</script>

<template>
  <div class="player-core">
    <!-- 专辑封面 -->
    <div class="cover" :class="{ playing: isPlaying }">
      <img v-if="coverUrl && !coverError" :src="coverUrl" class="cover-img"
           referrerpolicy="no-referrer"
           @load="coverLoaded = true"
           @error="coverError = true" />
      <span v-if="!coverUrl || coverError" class="cover-emoji">{{ coverEmoji }}</span>
    </div>

    <!-- 歌曲信息 -->
    <div class="track-info">
      <div class="track-name">{{ currentSong?.title || '未选择歌曲' }}</div>
      <div class="track-artist">{{ currentSong?.artist || '—' }}</div>
      <div v-if="currentSong?.album" class="track-album">💿 {{ currentSong.album }}</div>
    </div>

    <!-- 进度条 -->
    <div class="progress-section">
      <span class="time">{{ formattedCurrent }}</span>
      <div class="progress-bar" @click="seekTo">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        <div class="progress-thumb" :style="{ left: progressPercent + '%' }"></div>
      </div>
      <span class="time">{{ formattedDuration }}</span>
    </div>

    <!-- 播放控制 -->
    <div class="controls">
      <button class="ctrl-btn mode-btn" :title="modeLabel" @click="cycleMode">
        {{ modeIcon }}
      </button>
      <button class="ctrl-btn" title="上一首" @click="emit('prev')">⏮</button>
      <button class="ctrl-btn play-btn" :class="{ active: isPlaying }" @click="togglePlay">
        {{ isPlaying ? '⏸' : '▶' }}
      </button>
      <button class="ctrl-btn" title="下一首" @click="emit('next')">⏭</button>
      <div class="volume-group">
        <button class="ctrl-btn volume-icon" @click="toggleMute">
          {{ isMuted || volume === 0 ? '🔇' : volume < 0.5 ? '🔉' : '🔊' }}
        </button>
        <input type="range" class="volume-slider" min="0" max="1" step="0.05"
               :value="volume" @input="changeVolume" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.player-core { padding: 4px 0; }

/* 封面 */
.cover {
  width: 120px; height: 120px; border-radius: var(--radius-md);
  background: linear-gradient(135deg, #1a1a3e, #2a1a3e);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto; transition: transform 0.5s, box-shadow 0.5s; overflow: hidden;
  position: relative;
}
.cover.playing {
  animation: spin 8s linear infinite;
  box-shadow: 0 0 20px var(--color-primary-glow);
}
.cover-img { width: 100%; height: 100%; object-fit: cover; }
.cover-emoji { font-size: 48px; }

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 歌曲信息 */
.track-info { text-align: center; margin-top: 10px; }
.track-name { font-size: 15px; font-weight: 600; }
.track-artist { font-size: 12px; color: var(--color-text-muted); margin-top: 3px; }
.track-album { font-size: 11px; color: var(--color-text-dim); margin-top: 2px; }

/* 进度条 */
.progress-section {
  display: flex; align-items: center; gap: 8px; margin-top: 12px;
}
.time { font-size: 11px; color: var(--color-text-muted); min-width: 36px; text-align: center; }
.progress-bar {
  flex: 1; height: 5px; background: var(--color-border);
  border-radius: 3px; cursor: pointer; position: relative;
}
.progress-fill {
  height: 100%; background: var(--color-primary); border-radius: 3px;
  transition: width 0.15s; position: absolute; left: 0; top: 0;
}
.progress-thumb {
  width: 11px; height: 11px; border-radius: 50%; background: var(--color-primary);
  position: absolute; top: -3px; transform: translateX(-50%);
  box-shadow: 0 0 6px var(--color-primary-glow); opacity: 0; transition: opacity 0.2s;
}
.progress-bar:hover .progress-thumb { opacity: 1; }

/* 控制按钮 */
.controls {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; margin-top: 12px; flex-wrap: wrap;
}
.ctrl-btn {
  background: none; border: 1px solid var(--color-border);
  color: var(--color-text); border-radius: 50%;
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; font-size: 14px; transition: all 0.15s;
}
.ctrl-btn:hover { background: var(--color-surface-hover); border-color: var(--color-primary); }
.play-btn {
  width: 42px; height: 42px; font-size: 18px;
  background: var(--color-primary); border-color: var(--color-primary);
}
.play-btn:hover { box-shadow: var(--shadow-glow); }
.play-btn.active { background: #c0392b; }
.mode-btn { font-size: 12px; }

/* 音量 */
.volume-group { display: flex; align-items: center; gap: 4px; }
.volume-icon { border: none; width: 28px; height: 28px; font-size: 14px; }
.volume-slider {
  width: 60px; height: 4px; -webkit-appearance: none; appearance: none;
  background: var(--color-border); border-radius: 2px; outline: none; cursor: pointer;
}
.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none; width: 12px; height: 12px;
  border-radius: 50%; background: var(--color-text); cursor: pointer;
}
</style>
