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
