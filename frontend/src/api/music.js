// ===== 队员B — 音乐服务 API =====
import axios from 'axios'
import { getCurrentUserId } from './user'

// 音乐模块直连 8082（开发阶段 Vite 代理 /music → localhost:8082）
const musicApi = axios.create({
  baseURL: '/music',
  timeout: 10000
})

export { musicApi }

function resolveUserId(userId) {
  return userId || getCurrentUserId()
}

export default {
  // ── 歌曲 ──
  songList: (params) => musicApi.get('/song/list', { params }),
  songDetail: (id) => musicApi.get(`/song/${id}`),
  search: (kw, page = 1, size = 20) =>
    musicApi.get('/song/search', { params: { kw, page, size } }),
  genres: () => musicApi.get('/song/genres'),
  saveLyric: (id, lyric) =>
    musicApi.put(`/song/${id}/lyric`, { lyric }),
  analyzeSongEmotion: (id) =>
    musicApi.post(`/song/${id}/analyze-emotion`),

  // ── 歌单 ──
  playlistList: (userId) => musicApi.get('/playlist/list', { params: { userId: resolveUserId(userId) } }),
  playlistDetail: (id) => musicApi.get(`/playlist/${id}`),
  playlistSongs: (id) => musicApi.get(`/playlist/${id}/songs`),
  createPlaylist: (name, description = '', userId) =>
    musicApi.post('/playlist', { name, description, userId: resolveUserId(userId) }),
  updatePlaylist: (id, name, description) =>
    musicApi.put(`/playlist/${id}`, { name, description }),
  deletePlaylist: (id) => musicApi.delete(`/playlist/${id}`),
  addSongToPlaylist: (playlistId, songId) =>
    musicApi.post(`/playlist/${playlistId}/song/${songId}`),
  removeSongFromPlaylist: (playlistId, songId) =>
    musicApi.delete(`/playlist/${playlistId}/song/${songId}`),
  sortPlaylist: (playlistId, songIds) =>
    musicApi.put(`/playlist/${playlistId}/sort`, { songIds }),
  importPlaylist: (name, content, userId) =>
    musicApi.post('/playlist/import', { name, content, userId: resolveUserId(userId) }),

  // ── 收藏 ──
  favoriteList: (userId) => musicApi.get('/favorite/list', { params: { userId: resolveUserId(userId) } }),
  addFavorite: (songId, userId) =>
    musicApi.post(`/favorite/${songId}`, null, { params: { userId: resolveUserId(userId) } }),
  removeFavorite: (songId, userId) =>
    musicApi.delete(`/favorite/${songId}`, { params: { userId: resolveUserId(userId) } }),
  checkFavorite: (songId, userId) =>
    musicApi.get(`/favorite/check/${songId}`, { params: { userId: resolveUserId(userId) } }),

  // ── 播放历史 ──
  historyList: (userId) => musicApi.get('/history/list', { params: { userId: resolveUserId(userId) } }),
  recordPlay: (songId, userId) =>
    musicApi.post('/history', { songId, userId: resolveUserId(userId) }),

  // ── 网易云音乐 ──
  neteaseSearch: (keywords, limit = 20) =>
    musicApi.get('/netease/search', { params: { keywords, limit } }),
  neteaseUrl: (id) => musicApi.get('/netease/url', { params: { id } }),
  neteaseDetail: (ids) => musicApi.get('/netease/detail', { params: { ids } }),
  neteaseLyric: (id) => musicApi.get('/netease/lyric', { params: { id } }),
  neteasePing: () => musicApi.get('/netease/ping'),
  neteasePlaylist: (id) => musicApi.get('/netease/playlist', { params: { id } }),

  // ── 情绪分析 ──
  getSongEmotion: (songId) => musicApi.get(`/emotion/${songId}`),
  analyzeSong: (songId) => musicApi.post(`/emotion/analyze/${songId}`),
  batchAnalyze: (playlistId) => musicApi.post(`/emotion/batch/${playlistId}`),
  searchByEmotion: (tag) => musicApi.get('/emotion/search', { params: { tag } }),
  playlistEmotionOverview: (playlistId) => musicApi.get(`/emotion/playlist/${playlistId}/overview`),

  // ── 用户品味 ──
  getUserTaste: (userId) => musicApi.get(`/taste/${resolveUserId(userId)}`),
  refreshTaste: (userId) => musicApi.get(`/taste/refresh/${resolveUserId(userId)}`),

  // ── 健康检查 ──
  hello: () => musicApi.get('/hello')
}

/**
 * 将网易云封面 URL 改写为后端代理地址，绕过防盗链
 * @param {string} url - 原始封面 URL
 * @returns {string} 代理后的 URL，非网易云 URL 原样返回
 */
export function getCoverUrl(url) {
  if (!url) return ''
  // 网易云图片域名通过代理转发
  if (url.includes('music.126.net') || url.includes('126.net')) {
    return `/music/netease/cover?url=${encodeURIComponent(url)}`
  }
  // 其他 HTTP/HTTPS 图片直接使用
  return url
}
