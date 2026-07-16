<script setup>
/**
 * 👤 队员A — 智能对话 DJ 面板
 * 功能: AI对话 + WebSocket实时聊天 + DJ欢迎语(时段感知)
 * 后端: module-chat (:8081)
 */
import { ref, onMounted } from 'vue'
import { chatApi } from '../api'

const connected = ref(false)
const input = ref('')
const messages = ref([
  { role: 'dj', text: '下午好！我是你的 DJ 助手 🎧，想听点什么？', time: '' }
])

onMounted(async () => {
  try { await chatApi.hello(); connected.value = true } catch(e) {}
})

function send() {
  if (!input.value.trim()) return
  messages.value.push({ role: 'user', text: input.value, time: '刚刚' })
  // TODO: 通过 WebSocket 发给后端
  setTimeout(() => {
    messages.value.push({ role: 'dj', text: '好的，正在为你找歌... 🎵', time: '刚刚' })
  }, 500)
  input.value = ''
}
</script>

<template>
  <div class="panel chat-panel">
    <div class="panel-title">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      AI 对话 DJ
    </div>

    <!-- 消息区 -->
    <div class="messages">
      <div v-for="(m, i) in messages" :key="i"
           :class="['msg', m.role]">
        <span v-if="m.role === 'dj'" class="role">🎧 DJ</span>
        <span v-else class="role">👤 你</span>
        <p>{{ m.text }}</p>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-row">
      <el-input v-model="input" placeholder="说点什么..." @keyup.enter="send"
                size="small" :dark="true" />
      <button class="btn-primary" @click="send">发送</button>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex; flex-direction: column; overflow: hidden;
}
.messages {
  flex: 1; overflow-y: auto; padding: var(--gap-xs) 0;
  display: flex; flex-direction: column; gap: 10px;
  min-height: 0;
}
.msg { padding: 8px 12px; border-radius: var(--radius-sm); font-size: 13px; }
.msg.dj { background: rgba(233, 69, 96, 0.1); border-left: 2px solid var(--color-primary); }
.msg.user { background: rgba(15, 155, 255, 0.1); border-left: 2px solid var(--color-info); }
.msg p { margin-top: 4px; line-height: 1.5; }
.role { font-size: 11px; color: var(--color-text-muted); }

.input-row {
  display: flex; gap: var(--gap-xs); margin-top: var(--gap-sm);
  padding-top: var(--gap-xs); border-top: 1px solid var(--color-border);
}
.input-row :deep(.el-input__inner) {
  background: var(--color-bg); border-color: var(--color-border); color: var(--color-text);
}
</style>
