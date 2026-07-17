# DJ 音乐电台前端

前端使用 Vue 3、Vite、Element Plus，实现单屏 DJ 音乐控制台。

## 1. 真实入口

真实演示入口是：

```text
src/App.vue
```

`src/router/index.js` 和 `src/views/*` 当前保留旧占位页面，不是主要演示入口。

## 2. 主要组件

```text
App.vue
├── components/TimeWidget.vue
├── components/WeatherWidget.vue
├── components/ChatPanel.vue
├── components/MusicPanel.vue
│   └── components/music/*
├── components/RecPanel.vue
└── components/UserBar.vue
```

## 3. 安装和启动

```powershell
cd D:\projects\dj-music-radio\frontend
npm.cmd install
npm.cmd run dev -- --host 0.0.0.0
```

本机访问：

```text
http://127.0.0.1:5173/
```

Mac 通过远端 Windows 访问时，需要先运行端口转发脚本，详见仓库外的远端开发环境说明。

## 4. 构建

```powershell
cd D:\projects\dj-music-radio\frontend
npm.cmd run build
```

## 5. 代理

`vite.config.js` 当前配置：

```text
/api/**   -> http://localhost:8080/**
/music/** -> http://localhost:8082/music/**
```

因此前端完整体验通常需要启动：

- Gateway 8080；
- module-chat 8081；
- module-music 8082；
- module-rec 8083；
- module-user 8084；
- NeteaseCloudMusicApi 3000；
- MySQL、Redis、RabbitMQ、Nacos。

## 6. 关键交互

- ChatPanel 优先使用 WebSocket `/chat/ws`，失败时可走 REST `/chat/send`；
- 天气和聊天定位使用浏览器 `navigator.geolocation`；
- 聊天推荐歌曲点击后触发 `dj-play-song`；
- MusicPanel 监听 `dj-play-song` 并播放对应歌曲；
- UserBar 当前为本地登录状态 UI，后端真实用户模块尚未完成。
