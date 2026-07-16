// ===== 队员A — 对话服务 =====
import api from './index'

export const chatApi = {
  hello: () => api.get('/chat/hello'),
  send: (msg) => api.post('/chat/send', msg),
  history: (userId) => api.get('/chat/history', { params: { userId } }),
  weather: (city) => api.get('/chat/weather', { params: { city } })
}
