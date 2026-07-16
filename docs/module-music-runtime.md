# module-music 运行依赖说明

`module-music` 的代码在仓库中，但部分能力依赖本机额外服务和本地数据库数据。拉取代码后如果只启动 `module-music:8082`，网易云搜索、歌单导入、情绪分析等功能可能不会完整可用。

## 依赖总览

| 功能 | 必需依赖 | 端口/配置 | 说明 |
|:-----|:---------|:----------|:-----|
| 音乐服务本体 | `module-music` | `8082` | Spring Boot 服务 |
| 网易云搜索/播放/歌词/歌单 | `NeteaseCloudMusicApi` | `3000` | 独立 Node 服务，不在本仓库源码内 |
| 本地歌曲库/歌单/收藏/历史 | MySQL | `dj_music_radio` | 需要建库并执行 SQL |
| 歌单导入异步处理 | RabbitMQ | `5672` | `module-music` 启动时会连接 |
| 服务注册发现 | Nacos | `8848` | 网关转发需要服务注册 |
| AI 情绪分析 | DeepSeek API Key | `deepseek.api.key` | 不可提交真实 key |

## 网易云 API 代理

`NeteaseController.java` 中调用的是本机：

```java
http://localhost:3000
```

因此每个开发者本机都要启动 `NeteaseCloudMusicApi`。否则前端网易云 tab 会出现搜索失败，后端 `/music/netease/ping` 会返回连接拒绝。

Windows/远端开发环境可直接运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/netease/start-netease-api.ps1
```

脚本会把 npm 包安装到仓库本地 `.runtime/netease-api/`，并以前台进程启动 `node app.js`。`.runtime/` 已加入 `.gitignore`，不要提交第三方包或本地缓存。

如果只想安装不启动：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/netease/start-netease-api.ps1 -InstallOnly
```

检查是否可用：

```text
http://127.0.0.1:8080/music/netease/ping
http://127.0.0.1:8080/music/netease/search?keywords=周杰伦&limit=3
```

## 数据库初始化

`module-music` 使用独立数据库：`dj_music_radio`。

需要执行的 SQL 文件在：

```text
module-music/src/main/resources/init.sql
module-music/src/main/resources/emotion_schema.sql
module-music/src/main/resources/emotion_keywords.sql
```

表包括：

```text
song
playlist
playlist_song
user_favorite
play_history
song_emotion
emotion_keyword
user_taste
```

注意：本地歌曲、歌单、收藏、历史记录属于数据库数据，不会随 Git 同步。拉取代码后如果 `song` 表为空，`/music/song/list` 返回空数组是正常现象，需要先导入网易云歌单或歌曲。

## DeepSeek API Key

`application.yml` 中有 `deepseek.api.key` 配置。真实 key 不要提交到 GitHub。团队开发时建议使用本地环境变量或本地配置覆盖。

情绪分析逻辑会优先调用 DeepSeek；如果 DeepSeek 不可用，代码会回退到关键词分析。但如果要验收“AI 情绪分析真实可用”，必须确认日志中出现：

```text
调用DeepSeek API分析歌词
DeepSeek分析完成
```

## 常见问题
