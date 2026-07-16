import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 5000
})

// ===== 对话服务 =====
export const chatApi = {
  hello: () => api.get('/chat/hello')
}

// ===== 音乐服务 =====
export const musicApi = {
  hello: () => api.get('/music/hello')
}

// ===== 推荐服务 =====
export const recApi = {
  hello: () => api.get('/rec/hello')
}

// ===== 用户服务 =====
export const userApi = {
  hello: () => api.get('/user/hello')
}
