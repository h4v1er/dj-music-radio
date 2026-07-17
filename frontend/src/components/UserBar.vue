<script setup>
/**
 * 👤 队员D — 用户状态栏
 * 功能: 真实登录注册 + JWT 会话 + 收藏/历史联动 music 模块
 * 后端: module-user (:8084)
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { clearUserSession, getCurrentUser, saveUserSession, userApi } from '../api/user'

const connected = ref(false)
const loggedIn = ref(false)
const showLogin = ref(false)
const showFavorite = ref(false)
const showHistory = ref(false)
const showPassword = ref(false)
const isRegister = ref(false)
const loading = ref(false)
const passwordLoading = ref(false)
const user = ref(null)
const favorites = ref([])
const histories = ref([])

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', nickname: '', phone: '', email: '' })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const displayName = computed(() => user.value?.nickname || user.value?.username || '未登录')

onMounted(async () => {
  try {
    await userApi.hello()
    connected.value = true
  } catch (e) {
    connected.value = false
  }

  const stored = getCurrentUser()
  if (stored) {
    user.value = stored
    loggedIn.value = true
    refreshUserInfo()
  }

  window.addEventListener('dj-user-library-changed', handleLibraryChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('dj-user-library-changed', handleLibraryChanged)
})

async function refreshUserInfo() {
  try {
    const res = await userApi.info()
    if (res.data?.code === 200 && res.data.data) {
      const nextUser = {
        id: res.data.data.id,
        username: res.data.data.username,
        nickname: res.data.data.nickname || res.data.data.username,
        avatar: res.data.data.avatar || ''
      }
      localStorage.setItem('dj-user-profile', JSON.stringify(nextUser))
      user.value = nextUser
      loggedIn.value = true
    }
  } catch (e) {
    clearSession(false)
  }
}

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await userApi.login(loginForm.value)
    if (res.data?.code === 200) {
      user.value = saveUserSession(res.data.data)
      loggedIn.value = true
      showLogin.value = false
      loginForm.value.password = ''
      ElMessage.success('登录成功')
    } else {
      ElMessage.error(res.data?.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error('登录请求失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await userApi.register(registerForm.value)
    if (res.data?.code === 200) {
      ElMessage.success('注册成功，请登录')
      loginForm.value.username = registerForm.value.username
      loginForm.value.password = ''
      registerForm.value = { username: '', password: '', nickname: '', phone: '', email: '' }
      isRegister.value = false
    } else {
      ElMessage.error(res.data?.message || '注册失败')
    }
  } catch (e) {
    ElMessage.error('注册请求失败')
  } finally {
    loading.value = false
  }
}

function logout() {
  clearSession(true)
}

function handleUserCommand(command) {
  const actions = {
    favorites: loadFavorites,
    history: loadHistory,
    password: openPasswordDialog,
    logout
  }
  actions[command]?.()
}

async function handleLibraryChanged() {
  if (!loggedIn.value) return
  if (showFavorite.value) await loadFavorites()
  if (showHistory.value) await loadHistory()
}

function openPasswordDialog() {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  showPassword.value = true
}

async function handleChangePassword() {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) {
    ElMessage.warning('请完整填写密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 个字符')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  passwordLoading.value = true
  try {
    const res = await userApi.changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    if (res.data?.code === 200) {
      showPassword.value = false
      passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      ElMessage.success('密码已修改')
    } else {
      ElMessage.error(res.data?.message || '修改密码失败')
    }
  } catch (e) {
    ElMessage.error('修改密码请求失败')
  } finally {
    passwordLoading.value = false
  }
}

function clearSession(showMessage) {
  clearUserSession()
  user.value = null
  loggedIn.value = false
  favorites.value = []
  histories.value = []
  if (showMessage) ElMessage.info('已退出登录')
}

async function requireLogin() {
  if (loggedIn.value) return true
  showLogin.value = true
  ElMessage.warning('请先登录')
  return false
}

async function loadFavorites() {
  if (!(await requireLogin())) return
  showFavorite.value = true
  try {
    const res = await userApi.favoriteList()
    favorites.value = res.data?.code === 200 ? (res.data.data || []) : []
  } catch (e) {
    ElMessage.error('收藏加载失败')
  }
}

async function loadHistory() {
  if (!(await requireLogin())) return
  showHistory.value = true
  try {
    const res = await userApi.history()
    histories.value = res.data?.code === 200 ? (res.data.data || []) : []
  } catch (e) {
    ElMessage.error('历史加载失败')
  }
}

async function removeFavorite(song) {
  try {
    const res = await userApi.removeFavorite(song.id)
    if (res.data?.code === 200) {
      favorites.value = favorites.value.filter(item => item.id !== song.id)
      window.dispatchEvent(new CustomEvent('dj-user-session-changed', { detail: user.value }))
      ElMessage.success('已取消收藏')
    } else {
      ElMessage.error(res.data?.message || '取消收藏失败')
    }
  } catch (e) {
    ElMessage.error('取消收藏失败')
  }
}

function playSong(song) {
  window.dispatchEvent(new CustomEvent('dj-play-song', { detail: song }))
}
</script>

<template>
  <footer class="user-bar">
    <div class="left">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      <span v-if="connected" style="color:var(--color-success)">系统在线</span>
      <span v-else style="color:#666">连接中...</span>
      <span class="divider">|</span>
      <span>👤</span>
      <span v-if="loggedIn" class="username">{{ displayName }}</span>
      <span v-else style="color:var(--color-text-muted)">未登录</span>
    </div>

    <div class="right">
      <el-button v-if="!loggedIn" size="small" type="primary" plain class="login-btn" @click="showLogin = true">登录 / 注册</el-button>
      <el-dropdown v-else trigger="hover" placement="top-end" popper-class="user-account-popper" @command="handleUserCommand">
        <el-button size="small" plain class="account-menu-btn">
          <span class="account-menu-content">
            <span class="account-dot"></span>
            <span>账户操作</span>
          </span>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="favorites">❤ 我的收藏</el-dropdown-item>
            <el-dropdown-item command="history">播放历史</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="showLogin" :title="isRegister ? '注册' : '登录'" width="380px" class="user-dialog" modal-class="user-dialog-mask">
      <template v-if="!isRegister">
        <el-input v-model="loginForm.username" placeholder="用户名" style="margin-bottom:12px" />
        <el-input
          v-model="loginForm.password"
          placeholder="密码"
          type="password"
          show-password
          style="margin-bottom:16px"
          @keyup.enter="handleLogin"
        />
        <div class="dialog-actions">
          <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
          <el-button link type="info" @click="isRegister = true">没有账号？去注册</el-button>
        </div>
      </template>
      <template v-else>
        <el-input v-model="registerForm.username" placeholder="用户名 *" style="margin-bottom:10px" />
        <el-input v-model="registerForm.password" placeholder="密码 *" type="password" show-password style="margin-bottom:10px" />
        <el-input v-model="registerForm.nickname" placeholder="昵称" style="margin-bottom:10px" />
        <el-input v-model="registerForm.phone" placeholder="手机号" style="margin-bottom:10px" />
        <el-input v-model="registerForm.email" placeholder="邮箱" style="margin-bottom:16px" />
        <div class="dialog-actions">
          <el-button type="primary" :loading="loading" @click="handleRegister">注册</el-button>
          <el-button link type="info" @click="isRegister = false">已有账号？去登录</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="showFavorite" title="我的收藏" width="440px" class="user-dialog user-list-dialog" modal-class="user-dialog-mask">
      <div v-if="favorites.length === 0" class="empty-state">暂无收藏</div>
      <div v-for="song in favorites" :key="song.id" class="song-row">
        <button class="song-main" @click="playSong(song)">
          <span class="song-title">{{ song.title || '未知歌曲' }}</span>
          <span class="song-sub">{{ song.artist || '未知歌手' }}</span>
        </button>
        <el-button size="small" link type="danger" @click="removeFavorite(song)">取消</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="showHistory" title="播放历史" width="440px" class="user-dialog user-list-dialog" modal-class="user-dialog-mask">
      <div v-if="histories.length === 0" class="empty-state">暂无播放记录</div>
      <button v-for="song in histories" :key="song.id" class="history-row" @click="playSong(song)">
        <span class="song-title">{{ song.title || '未知歌曲' }}</span>
        <span class="song-sub">{{ song.artist || '未知歌手' }}</span>
      </button>
    </el-dialog>

    <el-dialog v-model="showPassword" title="修改密码" width="380px" class="user-dialog" modal-class="user-dialog-mask">
      <el-input
        v-model="passwordForm.oldPassword"
        placeholder="原密码"
        type="password"
        show-password
        style="margin-bottom:10px"
      />
      <el-input
        v-model="passwordForm.newPassword"
        placeholder="新密码，至少 6 个字符"
        type="password"
        show-password
        style="margin-bottom:10px"
      />
      <el-input
        v-model="passwordForm.confirmPassword"
        placeholder="确认新密码"
        type="password"
        show-password
        style="margin-bottom:16px"
        @keyup.enter="handleChangePassword"
      />
      <div class="dialog-actions">
        <el-button @click="showPassword = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">保存</el-button>
      </div>
    </el-dialog>
  </footer>
</template>

<style scoped>
.user-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 var(--gap-md);
  height: 40px; flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  font-size: 12px;
}
.left, .right { display: flex; align-items: center; gap: 10px; }
.dot { width: 7px; height: 7px; border-radius: 50%; }
.divider { color: var(--color-border); }
.username { color: var(--color-text); max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.login-btn,
.account-menu-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--color-border);
  --el-button-text-color: var(--color-text);
  --el-button-hover-bg-color: var(--color-surface-hover);
  --el-button-hover-border-color: var(--color-primary);
  --el-button-hover-text-color: var(--color-text);
}
.account-menu-btn {
  min-width: 98px;
}
.account-menu-content {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  line-height: 1;
}
.account-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 6px;
  border-radius: 50%;
  background: var(--color-success);
}
.dialog-actions { display: flex; justify-content: space-between; align-items: center; }
.empty-state {
  color: var(--color-text-muted);
  text-align: center;
  padding: 22px 0;
}
.song-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
}
.song-main,
.history-row {
  min-width: 0;
  flex: 1;
  border: 0;
  background: transparent;
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
  padding: 0;
}
.history-row {
  width: 100%;
  display: block;
  padding: 9px 0;
  border-bottom: 1px solid var(--color-border);
}
.song-main:hover .song-title,
.history-row:hover .song-title { color: var(--color-primary); }
.song-title {
  display: block;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.song-sub {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<style>
.user-account-popper.el-popper {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.user-account-popper .el-popper__arrow::before {
  border-color: var(--color-border);
  background: var(--color-surface);
}

.user-account-popper .el-dropdown-menu {
  min-width: 150px;
  padding: 6px;
  border: 0;
  background: var(--color-surface);
}

.user-account-popper .el-dropdown-menu__item {
  height: 34px;
  padding: 0 10px;
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 13px;
}

.user-account-popper .el-dropdown-menu__item:not(.is-disabled):focus,
.user-account-popper .el-dropdown-menu__item:not(.is-disabled):hover {
  background: var(--color-surface-hover);
  color: var(--color-text);
}

.user-account-popper .el-dropdown-menu__item--divided {
  border-top-color: var(--color-border);
}

.user-dialog-mask {
  background: rgba(5, 5, 12, 0.58);
  backdrop-filter: blur(3px);
}

.user-dialog.el-dialog {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.user-dialog .el-dialog__header {
  padding: 18px 22px 8px;
}

.user-dialog .el-dialog__title {
  color: var(--color-text);
  font-size: 18px;
  font-weight: 700;
}

.user-dialog .el-dialog__headerbtn .el-dialog__close {
  color: var(--color-text-muted);
}

.user-dialog .el-dialog__body {
  padding: 14px 22px 20px;
  color: var(--color-text);
}

.user-dialog .el-input__wrapper {
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.user-dialog .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.user-dialog .el-input__inner {
  color: var(--color-text);
}

.user-dialog .el-input__inner::placeholder {
  color: var(--color-text-dim);
}

.user-dialog .el-button--primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.user-list-dialog .el-dialog__body {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
