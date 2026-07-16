<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '../api'

const msg = ref('')
onMounted(async () => {
  try { msg.value = (await userApi.hello()).data } catch(e) { msg.value = '连接失败' }
})
</script>

<template>
  <div class="page">
    <h2>👤 用户中心</h2>
    <p class="desc">队员D 负责 · 端口 :8084 · Redis + JWT · Nacos/Gateway基础设施</p>
    <div class="box">
      <p>后端状态：{{ msg }}</p>
      <p style="color:#888;margin-top:12px">📌 TODO: 登录注册 + JWT鉴权 + 收藏管理</p>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
h2 { font-size: 24px; margin-bottom: 8px; }
.desc { color: #8888aa; margin-bottom: 24px; }
.box { padding: 24px; background: #1a1a2e; border-radius: 12px; border: 1px solid #2a2a4a; }
</style>
