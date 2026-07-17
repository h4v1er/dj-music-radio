// ===== 队员C — 推荐服务 =====
import api from './index'
import { getCurrentUserId } from './user'

export const recApi = {
  hello: () => api.get('/rec/hello'),
  daily: (userId) => api.get('/rec/daily', { params: { userId: userId || getCurrentUserId() } }),
  hot: () => api.get('/rec/hot'),
  similar: (songId) => api.get('/rec/similar', { params: { songId } }),
  preferences: (userId) => api.get('/rec/preferences', { params: { userId: userId || getCurrentUserId() } }),
  reportBehavior: (data) => api.post('/rec/behavior', { userId: getCurrentUserId(), ...data })
}
