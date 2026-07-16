// ===== 队员D — 用户服务 =====
import api from './index'

export const userApi = {
  hello: () => api.get('/user/hello'),
  register: (data) => api.post('/user/register', data),
  login: (data) => api.post('/user/login', data),
  getUserInfo: () => api.get('/user/info'),
  updateUserInfo: (data) => api.put('/user/info', data),
  addFavorite: (songId) => api.post('/user/favorite/add', { songId }),
  removeFavorite: (songId) => api.delete(`/user/favorite/${songId}`),
  history: () => api.get('/user/history')
}
