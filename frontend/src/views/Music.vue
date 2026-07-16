<script setup>
import { ref, onMounted } from 'vue'
import { musicApi } from '../api'

const msg = ref('')
onMounted(async () => {
  try { msg.value = (await musicApi.hello()).data } catch(e) { msg.value = '连接失败' }
})
</script>

<template>
  <div class="page">
    <h2>🎵 音乐中心</h2>
    <p class="desc">队员B 负责 · 端口 :8082 · MySQL + RabbitMQ</p>
    <div class="box">
      <p>后端状态：{{ msg }}</p>
      <p style="color:#888;margin-top:12px">📌 TODO: 音乐播放器 + 歌单管理 + 异步导入</p>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
h2 { font-size: 24px; margin-bottom: 8px; }
.desc { color: #8888aa; margin-bottom: 24px; }
.box { padding: 24px; background: #1a1a2e; border-radius: 12px; border: 1px solid #2a2a4a; }
</style>
