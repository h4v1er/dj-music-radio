// ===== 队员A — 对话服务 =====
import api from './index'

export const chatApi = {
  hello: () => api.get('/chat/hello'),
  send: (msg) => api.post('/chat/send', msg),
  history: (userId) => api.get('/chat/history', { params: { userId } }),
  weather: (city) => api.get('/chat/weather', { params: { city } }),
  createSocket: (userId) => new WebSocket(buildChatSocketUrl(userId))
}

function buildChatSocketUrl(userId) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.hostname || 'localhost'
  const port = window.location.port === '5173' ? '8080' : window.location.port
  const portPart = port ? `:${port}` : ''

  return `${protocol}//${host}${portPart}/chat/ws?userId=${encodeURIComponent(userId)}`
}
