<script setup>
/**
 * 歌单面板组件
 * 功能：歌单列表、创建/删除/重命名歌单、切换歌单
 */
import { ref, onMounted } from 'vue'
import api from '../../api/music'

const emit = defineEmits(['select', 'delete', 'refresh'])

const playlists = ref([])
const showCreate = ref(false)
const newName = ref('')
const editingId = ref(null)
const editingName = ref('')

onMounted(loadPlaylists)

async function loadPlaylists() {
  try {
    const res = await api.playlistList()
    playlists.value = res.data?.data || []
  } catch (e) { console.error('加载歌单失败', e) }
}

async function createPlaylist() {
  const name = newName.value.trim()
  if (!name) return
  try {
    await api.createPlaylist(name)
    newName.value = ''
    showCreate.value = false
    await loadPlaylists()
    emit('refresh')
  } catch (e) { console.error('创建歌单失败', e) }
}

async function deletePlaylist(id) {
  if (!confirm('确定删除此歌单？')) return
  try {
    await api.deletePlaylist(id)
    await loadPlaylists()
    emit('delete', id)
    emit('refresh')
  } catch (e) { console.error('删除歌单失败', e) }
}

function startEdit(pl) {
  editingId.value = pl.id
  editingName.value = pl.name
}

async function saveEdit(id) {
  const name = editingName.value.trim()
  if (!name) return
  try {
    await api.updatePlaylist(id, name, '')
    editingId.value = null
    await loadPlaylists()
    emit('refresh')
  } catch (e) { console.error('更新歌单失败', e) }
}

function cancelEdit() {
  editingId.value = null
}

function selectPlaylist(pl) {
  emit('select', pl)
}
</script>

<template>
  <div class="playlist-panel">
    <div class="section-title">📋 我的歌单</div>

    <!-- 歌单列表 -->
    <div class="playlist-items">
      <div v-for="pl in playlists" :key="pl.id" class="playlist-item"
           @click="selectPlaylist(pl)">
        <span>🎵</span>
        <template v-if="editingId === pl.id">
          <input v-model="editingName" class="edit-input" @keyup.enter="saveEdit(pl.id)"
                 @keyup.escape="cancelEdit" @click.stop autofocus />
          <button class="action-btn" @click.stop="saveEdit(pl.id)">✓</button>
          <button class="action-btn" @click.stop="cancelEdit">✗</button>
        </template>
        <template v-else>
          <div class="pl-info">
            <div class="pl-name">{{ pl.name }}</div>
            <div class="pl-count">{{ pl.songCount || 0 }} 首</div>
          </div>
          <button class="action-btn" title="重命名" @click.stop="startEdit(pl)">✎</button>
          <button class="action-btn del-btn" title="删除" @click.stop="deletePlaylist(pl.id)">✕</button>
        </template>
      </div>
      <div v-if="playlists.length === 0" class="empty-hint">暂无歌单</div>
    </div>

    <!-- 创建歌单 -->
    <div v-if="showCreate" class="create-form">
      <input v-model="newName" type="text" placeholder="歌单名称..." class="create-input"
             @keyup.enter="createPlaylist" @keyup.escape="showCreate = false" autofocus />
      <button class="btn btn-confirm" @click="createPlaylist">创建</button>
      <button class="btn btn-cancel" @click="showCreate = false">取消</button>
    </div>
    <button v-else class="btn-create" @click="showCreate = true">➕ 新建歌单</button>
  </div>
</template>

<style scoped>
.playlist-panel { }

.section-title { font-size: 12px; color: var(--color-text-muted); margin-bottom: 8px; }

.playlist-items { max-height: 180px; overflow-y: auto; }
.playlist-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: var(--radius-sm); cursor: pointer;
  transition: background 0.15s; font-size: 12px;
}
.playlist-item:hover { background: var(--color-surface-hover); }
.pl-info { flex: 1; min-width: 0; }
.pl-name { font-size: 13px; font-weight: 500; }
.pl-count { font-size: 11px; color: var(--color-text-muted); }
.edit-input {
  flex: 1; padding: 2px 6px; border: 1px solid var(--color-primary);
  border-radius: 3px; background: var(--color-bg); color: var(--color-text); font-size: 12px;
}
.action-btn {
  background: none; border: none; color: var(--color-text-muted);
  cursor: pointer; font-size: 12px; padding: 2px 4px;
}
.action-btn:hover { color: var(--color-text); }
.del-btn:hover { color: var(--color-primary); }
.empty-hint { text-align: center; padding: 16px; font-size: 13px; color: var(--color-text-muted); }

/* 创建 */
.btn-create {
  width: 100%; margin-top: 8px; padding: 7px;
  background: none; border: 1px dashed var(--color-border);
  color: var(--color-text-muted); border-radius: var(--radius-sm);
  font-size: 12px; cursor: pointer; transition: all 0.15s;
}
.btn-create:hover { border-color: var(--color-primary); color: var(--color-primary); }
.create-form { display: flex; gap: 6px; margin-top: 8px; }
.create-input {
  flex: 1; padding: 6px 8px; border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm); background: var(--color-bg);
  color: var(--color-text); font-size: 12px; outline: none;
}
.btn {
  padding: 6px 12px; border: none; border-radius: var(--radius-sm);
  font-size: 12px; cursor: pointer;
}
.btn-confirm { background: var(--color-primary); color: #fff; }
.btn-cancel { background: var(--color-border); color: var(--color-text-muted); }
</style>
