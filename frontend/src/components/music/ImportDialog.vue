<script setup>
/**
 * 歌单导入对话框
 * 功能：
 *   1. 粘贴网易云歌单链接 → 自动解析导入
 *   2. 手动粘贴 JSON 歌单数据 → 异步导入
 */
import { ref, computed } from 'vue'
import api from '../../api/music'

const emit = defineEmits(['close', 'imported'])

const visible = ref(false)
const mode = ref('url')  // 'url' | 'json'

// ── URL 模式 ──
const playlistUrl = ref('')
const urlLoading = ref(false)
const previewPlaylist = ref(null)  // { name, coverImgUrl, trackCount, songs[] }
const urlResult = ref('')
const importing = ref(false)

// ── JSON 模式 ──
const name = ref('')
const jsonContent = ref('')
const jsonResult = ref('')

const sampleJson = JSON.stringify([
  { "title": "示例歌曲1", "artist": "歌手A", "album": "专辑A", "genre": "流行", "duration": 240 },
  { "title": "示例歌曲2", "artist": "歌手B", "album": "专辑B", "genre": "摇滚", "duration": 280 },
  { "title": "示例歌曲3", "artist": "歌手C", "album": "专辑C", "genre": "电子", "duration": 200 }
], null, 2)

const titleText = computed(() => mode.value === 'url' ? '📎 粘贴网易云歌单链接' : '📋 手动输入JSON')

function open() {
  visible.value = true
  mode.value = 'url'
  playlistUrl.value = ''
  previewPlaylist.value = null
  urlResult.value = ''
  name.value = ''
  jsonContent.value = ''
  jsonResult.value = ''
  importing.value = false
}

function close() {
  visible.value = false
  emit('close')
}

// ── URL 解析 ──
function parsePlaylistId(url) {
  // 支持格式:
  // https://music.163.com/playlist?id=12345678&userid=xxx
  // https://music.163.com/#/playlist?id=12345678
  // https://y.music.163.com/m/playlist?id=12345678
  // 直接输入纯数字 ID
  const match = url.match(/[?&/#]id=(\d+)/)
  if (match) return match[1]
  if (/^\d+$/.test(url.trim())) return url.trim()
  return null
}

async function fetchPlaylist() {
  const url = playlistUrl.value.trim()
  if (!url) { urlResult.value = '⚠️ 请输入网易云歌单链接或歌单ID'; return }

  const id = parsePlaylistId(url)
  if (!id) {
    urlResult.value = '⚠️ 无法识别链接格式，请确认是网易云歌单分享链接\n支持格式: https://music.163.com/playlist?id=歌单ID'
    return
  }

  urlLoading.value = true
  urlResult.value = '⏳ 正在获取歌单信息...'
  previewPlaylist.value = null

  try {
    const res = await api.neteasePlaylist(id)
    const data = res.data?.data
    if (!data || !data.songs) {
      urlResult.value = '❌ 获取歌单失败，请确认歌单存在且可公开访问'
      urlLoading.value = false
      return
    }
    previewPlaylist.value = data
    urlResult.value = `✅ 找到歌单「${data.name}」，共 ${data.trackCount} 首歌曲`
  } catch (e) {
    urlResult.value = '❌ 获取失败: ' + (e.response?.data?.message || e.message)
  } finally {
    urlLoading.value = false
  }
}

// ── 从歌单预览导入 ──
async function importFromUrl() {
  if (!previewPlaylist.value) return
  const pl = previewPlaylist.value
  importing.value = true
  urlResult.value = '⏳ 正在导入...'

  try {
    // 构建 JSON 发给后端导入
    const songs = pl.songs.map(s => ({
      title: s.name,
      artist: s.artist,
      album: s.album || '',
      genre: s.genre || '网易云',  // 继承歌单风格标签
      duration: s.duration || 0,
      coverUrl: s.coverUrl || '',
      source: 'NETEASE',
      sourceId: String(s.id)
    }))
    const content = JSON.stringify(songs)
    await api.importPlaylist(pl.name, content)
    urlResult.value = `✅ 成功导入「${pl.name}」(${songs.length} 首)`
    setTimeout(() => {
      emit('imported')
      close()
    }, 1500)
  } catch (e) {
    urlResult.value = '❌ 导入失败: ' + (e.response?.data?.message || e.message)
  } finally {
    importing.value = false
  }
}

// ── JSON 导入 ──
async function doJsonImport() {
  const n = name.value.trim()
  const content = jsonContent.value.trim()
  if (!n || !content) {
    jsonResult.value = '⚠️ 请填写歌单名称和歌曲数据'
    return
  }
  try { JSON.parse(content) } catch (e) {
    jsonResult.value = '⚠️ JSON 格式不正确'
    return
  }
  importing.value = true
  jsonResult.value = '⏳ 正在导入...'
  try {
    await api.importPlaylist(n, content)
    jsonResult.value = '✅ 导入任务已提交，后台异步处理中...'
    setTimeout(() => {
      emit('imported')
      close()
    }, 1500)
  } catch (e) {
    jsonResult.value = '❌ 导入失败: ' + (e.response?.data?.message || e.message)
  } finally {
    importing.value = false
  }
}

function fillSample() {
  jsonContent.value = sampleJson
}

defineExpose({ open, close })
</script>

<template>
  <div v-if="visible" class="dialog-overlay" @click.self="close">
    <div class="dialog-box">
      <div class="dialog-header">
        <span>📥 导入歌单</span>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <!-- 模式切换 -->
      <div class="mode-tabs">
        <button class="mode-tab" :class="{ active: mode === 'url' }" @click="mode = 'url'">
          🔗 粘贴网易云链接
        </button>
        <button class="mode-tab" :class="{ active: mode === 'json' }" @click="mode = 'json'">
          📋 手动输入JSON
        </button>
      </div>

      <div class="dialog-body">
        <!-- ===== 链接模式 ===== -->
        <template v-if="mode === 'url'">
          <label class="field-label">网易云歌单分享链接</label>
          <div class="url-row">
            <input v-model="playlistUrl" type="text"
                   placeholder="https://music.163.com/playlist?id=歌单ID  或直接输入歌单ID"
                   class="field-input" @keyup.enter="fetchPlaylist" />
            <button class="btn-fetch" :disabled="urlLoading || !playlistUrl.trim()" @click="fetchPlaylist">
              {{ urlLoading ? '获取中...' : '获取歌单' }}
            </button>
          </div>
          <div class="url-hint">
            支持格式：<code>https://music.163.com/playlist?id=12345678</code> 或直接输入数字ID
          </div>

          <!-- 歌单预览 -->
          <div v-if="previewPlaylist" class="preview-card">
            <div class="preview-header">
              <img v-if="previewPlaylist.coverImgUrl"
                   :src="`/music/netease/cover?url=${encodeURIComponent(previewPlaylist.coverImgUrl)}`"
                   class="preview-cover" referrerpolicy="no-referrer"
                   @error="$event.target.style.display='none'" />
              <div class="preview-info">
                <div class="preview-name">{{ previewPlaylist.name }}</div>
                <div class="preview-count">
                  🎵 {{ previewPlaylist.trackCount }} 首
                  <span v-if="previewPlaylist.tags && previewPlaylist.tags.length" class="preview-tags">
                    ·
                    <span v-for="t in previewPlaylist.tags" :key="t" class="genre-tag">{{ t }}</span>
                  </span>
                </div>
              </div>
            </div>
            <div class="preview-songs">
              <div v-for="(s, i) in previewPlaylist.songs.slice(0, 8)" :key="s.id" class="preview-song">
                <span class="ps-idx">{{ i + 1 }}</span>
                <span class="ps-title">{{ s.name }}</span>
                <span class="ps-artist">{{ s.artist }}</span>
                <span v-if="s.genre" class="ps-genre">{{ s.genre }}</span>
              </div>
              <div v-if="previewPlaylist.songs.length > 8" class="ps-more">
                ... 还有 {{ previewPlaylist.songs.length - 8 }} 首
              </div>
            </div>
            <button class="btn-import" :disabled="importing" @click="importFromUrl">
              {{ importing ? '导入中...' : `📥 导入到我的歌单` }}
            </button>
          </div>

          <div v-if="urlResult" class="import-msg" :class="{ error: urlResult.startsWith('❌') || urlResult.startsWith('⚠️') }">
            {{ urlResult }}
          </div>
        </template>

        <!-- ===== JSON 模式 ===== -->
        <template v-if="mode === 'json'">
          <label class="field-label">歌单名称</label>
          <input v-model="name" type="text" placeholder="输入歌单名称..." class="field-input" />

          <label class="field-label">
            歌曲列表（JSON格式）
            <button class="sample-btn" @click="fillSample">📋 填入示例</button>
          </label>
          <textarea v-model="jsonContent" class="field-textarea"
                    placeholder='[{"title":"歌名","artist":"歌手","album":"专辑","genre":"流派","duration":240}]'
                    rows="8"></textarea>

          <div v-if="jsonResult" class="import-msg" :class="{ error: jsonResult.startsWith('❌') || jsonResult.startsWith('⚠️') }">
            {{ jsonResult }}
          </div>
        </template>
      </div>

      <!-- 底部按钮（JSON 模式） -->
      <div v-if="mode === 'json'" class="dialog-footer">
        <button class="btn-cancel" @click="close">取消</button>
        <button class="btn-import" :disabled="importing" @click="doJsonImport">
          {{ importing ? '导入中...' : '开始导入' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center;
}
.dialog-box {
  width: 520px; max-height: 85vh;
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); box-shadow: var(--shadow-panel);
  display: flex; flex-direction: column; overflow: hidden;
}
.dialog-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid var(--color-border);
  font-size: 14px; font-weight: 600;
}
.close-btn {
  background: none; border: none; color: var(--color-text-muted);
  cursor: pointer; font-size: 16px;
}
.close-btn:hover { color: var(--color-primary); }

/* 模式切换 */
.mode-tabs {
  display: flex; border-bottom: 1px solid var(--color-border);
}
.mode-tab {
  flex: 1; padding: 10px; background: none; border: none;
  color: var(--color-text-muted); font-size: 12px; cursor: pointer;
  transition: all 0.15s; border-bottom: 2px solid transparent;
}
.mode-tab:hover { color: var(--color-text); background: var(--color-surface-hover); }
.mode-tab.active {
  color: var(--color-primary); border-bottom-color: var(--color-primary);
  background: rgba(233,69,96,0.05);
}

.dialog-body { padding: 16px; flex: 1; overflow-y: auto; }
.field-label { font-size: 12px; color: var(--color-text-muted); margin: 8px 0 4px; display: block; }
.sample-btn {
  float: right; background: none; border: 1px solid var(--color-border);
  color: var(--color-text-muted); font-size: 11px; border-radius: 3px;
  cursor: pointer; padding: 1px 8px;
}
.sample-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.field-input {
  width: 100%; padding: 8px; border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); background: var(--color-bg);
  color: var(--color-text); font-size: 13px; outline: none;
}
.field-input:focus { border-color: var(--color-primary); }
.field-textarea {
  width: 100%; padding: 8px; border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); background: var(--color-bg);
  color: var(--color-text); font-size: 12px; outline: none;
  font-family: var(--font-mono); resize: vertical;
}
.field-textarea:focus { border-color: var(--color-primary); }
.import-msg {
  margin-top: 10px; font-size: 12px; padding: 8px 10px;
  border-radius: var(--radius-sm); white-space: pre-line;
  color: var(--color-success); background: rgba(78,204,163,0.08);
}
.import-msg.error { color: var(--color-primary); background: rgba(233,69,96,0.1); }

/* URL 模式 */
.url-row { display: flex; gap: 8px; }
.url-row .field-input { flex: 1; }
.btn-fetch {
  padding: 8px 16px; border: 1px solid var(--color-primary); border-radius: var(--radius-sm);
  background: none; color: var(--color-primary); font-size: 12px; cursor: pointer;
  white-space: nowrap; transition: all 0.15s;
}
.btn-fetch:hover:not(:disabled) { background: var(--color-primary); color: #fff; }
.btn-fetch:disabled { opacity: 0.4; cursor: not-allowed; }
.url-hint { margin-top: 6px; font-size: 11px; color: var(--color-text-dim); }
.url-hint code { color: var(--color-text-muted); background: var(--color-bg); padding: 1px 4px; border-radius: 3px; }

/* 歌单预览卡片 */
.preview-card {
  margin-top: 12px; border: 1px solid var(--color-border);
  border-radius: var(--radius-md); overflow: hidden;
}
.preview-header {
  display: flex; gap: 12px; padding: 12px;
  background: var(--color-bg); align-items: center;
}
.preview-cover {
  width: 60px; height: 60px; border-radius: var(--radius-sm); object-fit: cover;
  background: linear-gradient(135deg, #1a1a3e, #2a1a3e); flex-shrink: 0;
}
.preview-info { min-width: 0; }
.preview-name { font-size: 14px; font-weight: 600; }
.preview-count { font-size: 12px; color: var(--color-text-muted); margin-top: 2px; }
.preview-songs {
  max-height: 230px; overflow-y: auto; padding: 4px 0;
}
.preview-song {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 12px; font-size: 12px;
}
.preview-song:hover { background: var(--color-surface-hover); }
.ps-idx { width: 20px; text-align: center; color: var(--color-text-muted); flex-shrink: 0; }
.ps-title { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ps-artist { color: var(--color-text-muted); flex-shrink: 0; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ps-genre {
  flex-shrink: 0; font-size: 10px; padding: 1px 6px;
  border-radius: 8px; color: var(--color-primary); border: 1px solid var(--color-primary);
  opacity: 0.7;
}
.ps-more { text-align: center; padding: 6px; font-size: 11px; color: var(--color-text-dim); }
.btn-import {
  display: block; width: calc(100% - 24px); margin: 10px 12px; padding: 9px;
  border: none; border-radius: var(--radius-sm); font-size: 13px; cursor: pointer;
  background: var(--color-primary); color: #fff;
}
.btn-import:disabled { opacity: 0.5; cursor: not-allowed; }

/* JSON 模式 */
.dialog-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 12px 16px; border-top: 1px solid var(--color-border);
}
.btn-cancel {
  padding: 6px 18px; border: none; border-radius: var(--radius-sm);
  font-size: 13px; cursor: pointer;
  background: var(--color-border); color: var(--color-text-muted);
}
</style>
