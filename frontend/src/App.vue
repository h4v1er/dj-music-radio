<script setup>
import ChatPanel from './components/ChatPanel.vue'
import MusicPanel from './components/MusicPanel.vue'
import RecPanel from './components/RecPanel.vue'
import TimeWidget from './components/TimeWidget.vue'
import UserBar from './components/UserBar.vue'
import WeatherWidget from './components/WeatherWidget.vue'
</script>

<template>
  <!-- ===== 桌面式 DJ 控制台 — 单页面 ===== -->
  <div class="console">

    <!-- 顶部栏 -->
    <header class="topbar">
      <div class="logo">🎧 DJ 音乐电台</div>
      <div class="topbar-tools">
        <TimeWidget />
        <WeatherWidget />    <!-- 队员A: 天气 -->
      </div>
    </header>

    <!-- 主区域：三栏面板 + 可拖拽调整 -->
    <div class="main-area">

      <!-- 左侧：AI 对话面板  队员A -->
      <ChatPanel />

      <!-- 中间：音乐播放器    队员B -->
      <MusicPanel />

      <!-- 右侧：推荐面板      队员C -->
      <RecPanel />

    </div>

    <!-- 底部：用户状态栏  队员D -->
    <UserBar />

  </div>
</template>

<style scoped>
.console {
  display: flex; flex-direction: column;
  height: 100vh; overflow: hidden;
}

/* ---- 顶部栏 ---- */
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 var(--gap-md);
  height: 52px; flex-shrink: 0;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.logo {
  font-size: 20px; font-weight: 700;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.topbar-tools {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

/* ---- 主区域 ---- */
.main-area {
  flex: 1; display: grid;
  grid-template-columns: 280px 1fr 300px;
  gap: var(--gap-sm);
  padding: var(--gap-sm);
  overflow: hidden;
  min-height: 0;
}

/* ---- 响应式：小屏时堆叠 ---- */
@media (max-width: 1000px) {
  .main-area {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }
}
</style>
