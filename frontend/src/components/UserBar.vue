<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '../api/user'
import { ElMessage } from 'element-plus'

const connected = ref(false)
const loggedIn = ref(false)
const showLogin = ref(false)
const showFavorite = ref(false)
const showHistory = ref(false)
const isRegister = ref(false)

const user = ref(null)
const favorites = ref([])
const histories = ref([])

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', nickname: '', phone: '', email: '' })

onMounted(async () => {
  try { await userApi.hello(); connected.value = true } catch(e) {}
  const token = localStorage.getItem('token')
  if (token) {
    try {
      const res = await userApi.info()
      if (res.data.code === 200) {
        user.value = res.data.data
        loggedIn.value = true
      }
    } catch(e) {}
  }
})

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  try {
    const res = await userApi.login(loginForm.value)
    if (res.data.code === 200) {
      const vo = res.data.data
      localStorage.setItem('token', vo.token)
      user.value = { id: vo.userId, username: vo.username }
      loggedIn.value = true
      showLogin.value = false
      ElMessage.success('登录成功')
    } else {
      ElMessage.error(res.data.message || '登录失败')
    }
  } catch(e) {
    ElMessage.error('请求失败')
  }
}

async function handleRegister() {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  try {
    const res = await userApi.register(registerForm.value)
    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      loginForm.value.username = registerForm.value.username
      loginForm.value.password = ''
    } else {
      ElMessage.error(res.data.message || '注册失败')
    }
  } catch(e) {
    ElMessage.error('请求失败')
  }
}

function logout() {
  localStorage.removeItem('token')
  user.value = null
  loggedIn.value = false
  ElMessage.info('已退出登录')
}

async function loadFavorites() {
  showFavorite.value = true
  try {
    const res = await userApi.favoriteList()
    if (res.data.code === 200) {
      favorites.value = res.data.data
    }
  } catch(e) {}
}

async function loadHistory() {
  showHistory.value = true
  try {
    const res = await userApi.history()
    if (res.data.code === 200) {
      histories.value = res.data.data
    }
  } catch(e) {}
}

async function removeFav(songId) {
  try {
    const res = await userApi.removeFavorite(songId)
    if (res.data.code === 200) {
      favorites.value = favorites.value.filter(f => f.songId !== songId)
      ElMessage.success('已取消收藏')
    }
  } catch(e) {}
}
</script>

<template>
  <footer class="user-bar">
    <div class="left">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      <span v-if="connected" style="color:var(--color-success)">在线</span>
      <span v-else style="color:#666">连接中</span>
      <span class="divider">|</span>
      <span>👤</span>
      <span v-if="loggedIn" style="color:var(--color-text)">{{ user?.username }}</span>
      <span v-else style="color:var(--color-text-muted)">未登录</span>
    </div>

    <div class="right">
      <el-button v-if="!loggedIn" size="small" type="primary" plain
                 @click="showLogin = true">登录 / 注册</el-button>
      <template v-else>
        <el-button size="small" plain @click="loadFavorites">❤️ 收藏</el-button>
        <el-button size="small" plain @click="loadHistory">📜 历史</el-button>
        <el-button size="small" plain @click="logout">退出</el-button>
      </template>
    </div>

    <!-- 登录/注册弹窗 -->
    <el-dialog v-model="showLogin" :title="isRegister ? '注册' : '登录'" width="380px">
      <template v-if="!isRegister">
        <el-input v-model="loginForm.username" placeholder="用户名" style="margin-bottom:12px" />
        <el-input v-model="loginForm.password" placeholder="密码" type="password" show-password
                  @keyup.enter="handleLogin" style="margin-bottom:16px" />
        <div style="display:flex;justify-content:space-between;align-items:center">
          <el-button type="primary" @click="handleLogin">登录</el-button>
          <el-button link type="info" @click="isRegister = true">没有账号？去注册</el-button>
        </div>
      </template>
      <template v-else>
        <el-input v-model="registerForm.username" placeholder="用户名 *" style="margin-bottom:10px" />
        <el-input v-model="registerForm.password" placeholder="密码 *" type="password" show-password style="margin-bottom:10px" />
        <el-input v-model="registerForm.nickname" placeholder="昵称" style="margin-bottom:10px" />
        <el-input v-model="registerForm.phone" placeholder="手机号" style="margin-bottom:10px" />
        <el-input v-model="registerForm.email" placeholder="邮箱" style="margin-bottom:16px" />
        <div style="display:flex;justify-content:space-between;align-items:center">
          <el-button type="primary" @click="handleRegister">注册</el-button>
          <el-button link type="info" @click="isRegister = false">已有账号？去登录</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 收藏弹窗 -->
    <el-dialog v-model="showFavorite" title="❤️ 我的收藏" width="400px">
      <div v-if="favorites.length === 0" style="color:var(--color-text-muted);text-align:center;padding:20px">
        暂无收藏
      </div>
      <div v-for="f in favorites" :key="f.id" class="fav-item">
        <span>🎵 歌曲 #{{ f.songId }}</span>
        <el-button size="small" link type="danger" @click="removeFav(f.songId)">取消</el-button>
      </div>
    </el-dialog>

    <!-- 历史弹窗 -->
    <el-dialog v-model="showHistory" title="📜 播放历史" width="400px">
      <div v-if="histories.length === 0" style="color:var(--color-text-muted);text-align:center;padding:20px">
        暂无播放记录
      </div>
      <div v-for="h in histories" :key="h.id" class="fav-item">
        <span>🎵 歌曲 #{{ h.songId }}</span>
        <span style="color:var(--color-text-muted);font-size:12px">{{ h.playTime }}</span>
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
.dot {
  width: 7px; height: 7px; border-radius: 50%;
}
.divider { color: var(--color-border); }
.fav-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0; border-bottom: 1px solid var(--color-border);
}
</style>
