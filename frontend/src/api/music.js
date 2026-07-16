// ===== 队员B — 音乐服务 =====
import api from './index'

export const musicApi = {
  hello: () => api.get('/music/hello'),
  songList: (params) => api.get('/music/song/list', { params }),
  songDetail: (id) => api.get(`/music/song/${id}`),
  search: (kw) => api.get('/music/song/search', { params: { kw } }),
  playlistList: (userId) => api.get('/music/playlist/list', { params: { userId } }),
  createPlaylist: (data) => api.post('/music/playlist/create', data),
  importPlaylist: (data) => api.post('/music/playlist/import', data)
}
