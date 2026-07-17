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
- MusicPanel 会把当前播放队列、当前歌曲和播放模式持久化到 `/music/queue/state`，刷新页面后按当前用户恢复；
- RecPanel 支持手动刷新今日推荐，并会根据当前用户 ID 加载每日推荐和偏好标签；
- ChatPanel 支持放大/收回布局，PlayerCore 支持点击封面进入完整播放详情；
- UserBar 调用真实 user 接口完成登录/注册/退出/修改密码，并通过 `dj-user-session-changed` 通知 ChatPanel、MusicPanel、RecPanel 使用当前用户 ID 刷新数据。
