# 队员B 开发日志 — 音乐中心模块

> 姓名：______  分支：`dev-music`  后端：`module-music` (:8082)

## 负责内容

- 音乐播放器面板；
- 本地歌曲列表、搜索、详情；
- 网易云搜索、播放 URL、歌词、歌单；
- 歌单创建、编辑、删除、排序、导入；
- RabbitMQ 异步导入；
- 歌词情绪分析；
- 收藏和播放历史；
- 用户品味画像；
- 用户播放队列和播放器状态持久化。

## 阶段记录

- [x] 搭建 `module-music`，完成 `/music/hello` 健康检查；
- [x] 完成歌曲接口：`/music/song/list`、`/music/song/{id}`、`/music/song/search`、`/music/song/genres`；
- [x] 完成歌单接口：列表、详情、创建、更新、删除、加歌、移除、排序；
- [x] 完成 `PlaylistSongMapper.xml`，支持歌单歌曲排序和关联查询；
- [x] 完成 RabbitMQ 歌单导入链路：`/music/playlist/import` -> `music.exchange` -> `PlaylistImportListener`；
- [x] 接入 `NeteaseCloudMusicApi:3000`，支持搜索、播放 URL、详情、歌词、批量歌词、歌单、封面代理、健康检查；
- [x] 完成歌词保存接口和歌词展示前端；
- [x] 完成 DeepSeek 歌词情绪分析，失败时回退关键词规则分析；
- [x] 建立情绪标签体系和关键词库：`emotion_schema.sql`、`emotion_keywords.sql`；
- [x] 完成歌曲情绪、歌单情绪总览、按情绪标签搜索；
- [x] 完成收藏接口：收藏、取消、检查、列表；
- [x] 完成播放历史接口：记录播放、查询历史；
- [x] 完成用户播放队列持久化接口：查询、保存、清空当前用户队列状态；
- [x] 完成用户品味画像和刷新接口；
- [x] 前端 `MusicPanel.vue` 整合播放器、歌曲列表、歌单、歌词、情绪信息和导入弹窗；
- [x] 支持从聊天推荐点击歌曲后，通过 `dj-play-song` 事件交给播放器播放。
- [x] 前端刷新后可按当前用户恢复播放队列、当前歌曲和播放模式。

## 当前注意事项

- 网易云播放 URL 受版权、VIP、地区和代理状态影响，部分歌曲不可播放是正常外部限制；
- `NeteaseCloudMusicApi:3000` 不在仓库源码中，需通过 `scripts/netease/start-netease-api.ps1` 启动；
- `init.sql` 会重建 music 主表，已有演示数据不要随意重复执行；
- DeepSeek key 未配置时，歌词情绪分析会走关键词兜底；
- 当前 music 统一维护收藏、历史和播放队列，user 模块只作为带 JWT 的用户入口调用 music，避免重复数据源。
