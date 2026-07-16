<script setup>
import { ref, onMounted } from 'vue'
import { chatApi, musicApi, recApi, userApi } from '../api'

const results = ref({})

onMounted(async () => {
  try { results.value.chat = (await chatApi.hello()).data } catch(e) { results.value.chat = '❌' }
  try { results.value.music = (await musicApi.hello()).data } catch(e) { results.value.music = '❌' }
  try { results.value.rec = (await recApi.hello()).data } catch(e) { results.value.rec = '❌' }
  try { results.value.user = (await userApi.hello()).data } catch(e) { results.value.user = '❌' }
})
</script>

<template>
  <div class="home">
    <div class="hero">
      <h1>🎧 欢迎使用 DJ 音乐电台</h1>
      <p>对话式 AI 音乐推荐 · 天气感知 · 个性化歌单</p>
    </div>

    <div class="status-grid">
      <div class="card">💬 对话服务 <span :class="results.chat ? 'ok' : ''">{{ results.chat || '...' }}</span></div>
      <div class="card">🎵 音乐服务 <span :class="results.music ? 'ok' : ''">{{ results.music || '...' }}</span></div>
      <div class="card">📊 推荐服务 <span :class="results.rec ? 'ok' : ''">{{ results.rec || '...' }}</span></div>
      <div class="card">👤 用户服务 <span :class="results.user ? 'ok' : ''">{{ results.user || '...' }}</span></div>
    </div>
  </div>
</template>

<style scoped>
.home { max-width: 900px; margin: 0 auto; }
.hero {
  text-align: center; padding: 60px 0 40px;
}
.hero h1 { font-size: 36px; margin-bottom: 12px;
  background: linear-gradient(135deg, #e94560, #f5a623);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.hero p { color: #8888aa; font-size: 16px; }

.status-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.card {
  padding: 24px; border-radius: 12px; background: #1a1a2e;
  border: 1px solid #2a2a4a; font-size: 16px;
  display: flex; justify-content: space-between; align-items: center;
}
.card .ok { color: #4ecca3; font-size: 13px; }
</style>
