import request from './index'

export const userApi = {
  login(data) {
    return request({ url: '/user/login', method: 'post', data })
  },
  register(data) {
    return request({ url: '/user/register', method: 'post', data })
  },
  info() {
    return request({ url: '/user/info', method: 'get', headers: { Authorization: localStorage.getItem('token') || '' } })
  },
  updateInfo(data) {
    return request({ url: '/user/info', method: 'put', data, headers: { Authorization: localStorage.getItem('token') || '' } })
  },
  addFavorite(songId) {
    return request({ url: `/user/favorite/add?songId=${songId}`, method: 'post', headers: { Authorization: localStorage.getItem('token') || '' } })
  },
  removeFavorite(songId) {
    return request({ url: `/user/favorite/${songId}`, method: 'delete', headers: { Authorization: localStorage.getItem('token') || '' } })
  },
  favoriteList() {
    return request({ url: '/user/favorite/list', method: 'get', headers: { Authorization: localStorage.getItem('token') || '' } })
  },
  history() {
    return request({ url: '/user/history', method: 'get', headers: { Authorization: localStorage.getItem('token') || '' } })
  },
  hello() {
    return request({ url: '/user/hello', method: 'get' })
  }
}
