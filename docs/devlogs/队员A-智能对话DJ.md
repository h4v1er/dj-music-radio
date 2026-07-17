# 队员A 开发日志 — 智能对话DJ模块

> 姓名：______  分支：`dev-chat`  后端：module-chat (:8081)

---

## 负责内容
- 💬 AI 对话面板（ChatPanel.vue）
- ☀️ 天气小部件（WeatherWidget.vue）
- WebSocket 实时通信
- 时段感知 DJ 欢迎语
- 天气 API 接入（和风天气）

---

## 第1天（日期：2026-07-16）
- [x] 从 `master` 创建队员A开发分支 `dev-chat`
- [x] 完成 `POST /chat/send` 基础对话接口，支持关键词生成 DJ 回复和示例歌曲推荐
- [x] 完成 `GET /chat/history` 最近消息查询接口，当前阶段使用内存保存最近 10 条记录
- [x] 完成 `GET /chat/weather` 天气展示接口，先返回演示天气数据和时段问候
- [x] `ChatPanel.vue` 接入真实后端接口，替换前端假回复逻辑
- [x] `WeatherWidget.vue` 接入天气接口，展示城市、温度、天气和问候语
- [x] 验证通过：前端 `npm run build`、后端 Maven 打包、Gateway `/chat/send` `/chat/history` `/chat/weather` 联调
- [x] 新增 `ChatService`，REST 和 WebSocket 共用对话回复、推荐歌曲和历史记录逻辑
- [x] 新增 `/chat/ws` WebSocket 端点，支持 `{ type, userId, content }` 消息并返回 `{ type, content, songs, time }`
- [x] `ChatPanel.vue` 优先使用 WebSocket 实时对话，连接不可用时保留 REST 降级发送
- [x] 验证通过：前端构建、完整 Maven 打包、Gateway WebSocket `ws://127.0.0.1:8080/chat/ws` 收发消息、前端 5173 可访问
- [x] 新增 `chat_history` 表脚本、`ChatHistory` 实体和 `ChatHistoryMapper`
- [x] `module-chat` 接入 MyBatis Plus 和 MySQL 配置，`ChatService` 支持优先读写数据库、异常时内存降级
- [x] 验证通过：`module-chat -am package -DskipTests`，Gateway REST/WS 对话接口正常返回，`/chat/history` 返回最近消息
- [x] 配置 `MYSQL_PASSWORD` 环境变量，执行 `chat_history.sql` 建表，验证重启 `module-chat` 后仍能从 MySQL 读回历史记录
- [x] 优化 `ChatPanel.vue` 对话面板样式，调整左右消息气泡、连接状态、输入区和发送按钮
- [x] 新增 `WeatherService`，支持通过 `QWEATHER_API_KEY` 接入和风天气城市查询和实时天气接口
- [x] `WeatherWidget.vue` 增加加载态、失败态和刷新按钮；未配置天气 API key 时自动显示演示天气
- [x] 验证通过：前端构建、`module-chat -am package -DskipTests`、Gateway `/chat/weather?city=北京` 返回演示降级数据

## 第2天（日期：______）
- [ ] 

## 第3天（日期：______）
- [ ] 

## 第4天（日期：______）
- [ ] 


## 2026-07-16 补充记录
- [x] 新增 `MusicRecommendationClient` 和 `RecRecommendationClient`，通过 OpenFeign 预接入 module-music/module-rec 推荐能力
- [x] `ChatService` 优先调用远程推荐接口，队友接口未完成或不可用时自动降级到本地 3 首歌曲推荐
- [x] 验证通过：`module-chat -am package -DskipTests`、启动 8081、Gateway `/chat/send` 返回推荐列表、`/chat/history` 可查询消息
- [x] 新增 `DeepSeekChatClient`，复用 DeepSeek OpenAI 兼容接口，对用户消息做意图解析（情绪、场景、曲风、歌手、搜索关键词、是否需要推荐）
- [x] `ChatService` 改为 AI 意图解析 → 调用 music/rec 获取真实歌曲 → AI 生成 DJ 回复；无 DeepSeek key 或调用失败时保留原规则兜底
- [x] `module-chat` 新增 `DEEPSEEK_API_KEY` / `DEEPSEEK_API_URL` / `DEEPSEEK_MODEL` 环境变量配置，避免把 key 继续写入新模块配置
- [x] 验证通过：`module-chat -am package -DskipTests`；Gateway `/chat/send` 音乐意图请求返回 3 首歌，普通问候返回 `songs=[]`
- [x] 修复 AI 对话边界：DeepSeek 意图解析传入最近对话历史，支持“还有啥”“讲吧”等短追问；普通聊天/讲故事不再强制回到歌单推荐
- [x] 验证通过：`/chat/send` 连续对话中，音乐追问保留 `songs=3`，讲故事和继续讲故事返回 `songs=0`

## 2026-07-17 补充记录
- [x] 明确天气模块真实/演示数据边界：`GET /chat/weather` 新增 `message` 字段，返回和风天气实时数据或演示降级原因
- [x] `WeatherService` 支持 `QWEATHER_API_HOST`，兼容和风天气新版专属 API Host；保留 `QWEATHER_WEATHER_URL` / `QWEATHER_GEO_URL` 手动覆盖能力
- [x] `WeatherWidget.vue` 显示“实时 / 演示数据”来源标识，鼠标悬停可查看后端返回的来源说明
- [x] 新增 [module-chat-runtime.md](../module-chat-runtime.md)，记录 DeepSeek、和风天气环境变量、远端计划任务重启和天气接口验证方式
