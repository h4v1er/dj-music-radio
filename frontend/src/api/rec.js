// ===== 队员C — 推荐服务 =====
import api from './index'

export const recApi = {
  hello: () => api.get('/rec/hello'),
  daily: (userId) => api.get('/rec/daily', { params: { userId } }),
  hot: () => api.get('/rec/hot'),
  similar: (songId) => api.get('/rec/similar', { params: { songId } }),
  reportBehavior: (data) => api.post('/rec/behavior', data)
}
