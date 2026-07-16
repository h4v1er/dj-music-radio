<script setup>
/**
 * 👤 队员D — 用户状态栏 + 登录弹窗
 * 功能: 登录注册(JWT) + 收藏管理 + 播放历史 + Nacos/Gateway基础设施
 * 后端: module-user (:8084)  Redis + JWT
 */
import { ref, onMounted } from 'vue'
import { userApi } from '../api/user'

const connected = ref(false)
const loggedIn = ref(false)
const showLogin = ref(false)

onMounted(async () => {
  try { await userApi.hello(); connected.value = true } catch(e) {}
})
</script>

<template>
  <footer class="user-bar">
    <!-- 左侧状态 -->
    <div class="left">
      <span class="dot" :style="{ background: connected ? 'var(--color-success)' : '#666' }"></span>
      <span v-if="connected" style="color:var(--color-success)">系统在线</span>
      <span v-else style="color:#666">连接中...</span>
      <span class="divider">|</span>
      <span>👤</span>
      <span v-if="loggedIn" style="color:var(--color-text)">用户名</span>
      <span v-else style="color:var(--color-text-muted)">未登录</span>
    </div>

    <!-- 右侧 -->
    <div class="right">
      <el-button v-if="!loggedIn" size="small" type="primary" plain
                 @click="showLogin = true">登录</el-button>
      <template v-else>
        <el-button size="small" plain>❤️ 收藏</el-button>
        <el-button size="small" plain>📜 历史</el-button>
        <el-button size="small" plain>退出</el-button>
      </template>
    </div>

    <!-- 登录弹窗 TODO: 队员D 实现 -->
    <el-dialog v-model="showLogin" title="登录" width="360px">
      <el-input placeholder="用户名" style="margin-bottom:12px" />
      <el-input placeholder="密码" type="password" show-password />
      <template #footer>
        <el-button @click="showLogin = false">取消</el-button>
        <el-button type="primary" @click="loggedIn = true; showLogin = false">登录</el-button>
      </template>
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
</style>
