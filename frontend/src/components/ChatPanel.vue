<script setup>
/**
 * 👤 队员A — 智能对话 DJ 面板
 * 功能: AI对话 + WebSocket实时聊天 + DJ欢迎语(时段感知)
 * 后端: module-chat (:8081)
 */
import { nextTick, onMounted, ref } from 'vue'
import { chatApi } from '../api/chat'

const USER_ID = 1

const connected = ref(false)
const input = ref('')
const sending = ref(false)
const messages = ref([])
const messagesRef = ref(null)

onMounted(async () => {
  try {
    await chatApi.hello()
    connected.value = true
    const res = await chatApi.history(USER_ID)
    messages.value = normalizeMessages(res.data)
    await scrollToBottom()
  } catch (e) {
    connected.value = false
    messages.value = [
      { role: 'dj', text: 'DJ 服务暂时连不上，请稍后再试。', time: '' }
    ]
  }
})

async function send() {
  const content = input.value.trim()
  if (!content || sending.value) return

  messages.value.push({ role: 'user', text: content, time: '刚刚' })
  input.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const res = await chatApi.send({ userId: USER_ID, content })
    const reply = res.data.reply
    const songs = res.data.songs || []
    messages.value.push({
      role: reply.role || 'dj',
      text: formatReply(reply.text, songs),
      time: reply.time || '刚刚'
    })
    connected.value = true
  } catch (e) {
    connected.value = false
    messages.value.push({
      role: 'dj',
      text: '刚才没有连上后端，我稍后再帮你推荐。',
      time: '刚刚'
    })
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function normalizeMessages(list) {
  if (!Array.isArray(list) || list.length === 0) {
    return [{ role: 'dj', text: '下午好！我是你的 DJ 助手，想听点什么？', time: '' }]
  }
  return list
}

function formatReply(text, songs) {
  if (!songs.length) return text
  return `${text}\n${songs.map((song, index) => `${index + 1}. ${song}`).join('\n')}`
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<template>
  <div class="panel chat-panel">
    <div class="panel-title">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      AI 对话 DJ
    </div>

    <!-- 消息区 -->
    <div ref="messagesRef" class="messages">
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
                size="small" :disabled="sending" :dark="true" />
      <button class="btn-primary" :disabled="sending" @click="send">
        {{ sending ? '发送中' : '发送' }}
      </button>
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
.msg p { margin-top: 4px; line-height: 1.5; white-space: pre-line; }
.role { font-size: 11px; color: var(--color-text-muted); }

.input-row {
  display: flex; gap: var(--gap-xs); margin-top: var(--gap-sm);
  padding-top: var(--gap-xs); border-top: 1px solid var(--color-border);
}
.input-row :deep(.el-input__inner) {
  background: var(--color-bg); border-color: var(--color-border); color: var(--color-text);
}
.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}
</style>
