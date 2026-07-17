// ===== 队员D — 用户服务 =====
import api from './index'

export const USER_TOKEN_KEY = 'dj-user-token'
export const USER_PROFILE_KEY = 'dj-user-profile'

export function getAuthToken() {
  return localStorage.getItem(USER_TOKEN_KEY) || localStorage.getItem('token') || ''
}

export function getCurrentUser() {
  const raw = localStorage.getItem(USER_PROFILE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    localStorage.removeItem(USER_PROFILE_KEY)
    return null
  }
}

export function getCurrentUserId() {
  return getCurrentUser()?.id || 1
}

export function saveUserSession(loginVO) {
  const user = {
    id: loginVO.userId,
    username: loginVO.username,
    nickname: loginVO.nickname || loginVO.username,
    avatar: loginVO.avatar || ''
  }
  localStorage.setItem(USER_TOKEN_KEY, loginVO.token)
  localStorage.removeItem('token')
  localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(user))
  window.dispatchEvent(new CustomEvent('dj-user-session-changed', { detail: user }))
  return user
}

export function clearUserSession() {
  localStorage.removeItem(USER_TOKEN_KEY)
  localStorage.removeItem(USER_PROFILE_KEY)
  localStorage.removeItem('token')
  window.dispatchEvent(new CustomEvent('dj-user-session-changed', { detail: null }))
}

function authConfig(config = {}) {
  const token = getAuthToken()
  return {
    ...config,
    headers: {
      ...(config.headers || {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  }
}

export const userApi = {
  hello: () => api.get('/user/hello'),
  register: (data) => api.post('/user/register', data),
  login: (data) => api.post('/user/login', data),
  info: () => api.get('/user/info', authConfig()),
  updateInfo: (data) => api.put('/user/info', data, authConfig()),
  changePassword: (data) => api.put('/user/password', data, authConfig()),
  addFavorite: (songId) => api.post(`/user/favorite/${songId}`, null, authConfig()),
  removeFavorite: (songId) => api.delete(`/user/favorite/${songId}`, authConfig()),
  favoriteList: () => api.get('/user/favorite/list', authConfig()),
  history: () => api.get('/user/history', authConfig()),
  addHistory: (songId) => api.post('/user/history', { songId }, authConfig())
}
