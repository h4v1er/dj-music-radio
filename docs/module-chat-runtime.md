# module-chat 运行与实现说明

负责范围：队员 A 智能对话 DJ、天气、时间、定位、ChatPanel。

## 1. 当前能力

- `GET /chat/hello` 健康检查；
- `POST /chat/send` REST 对话；
- `GET /chat/history` 最近 10 条历史；
- `GET /chat/weather` 和风天气或演示降级；
- `/chat/ws` WebSocket 实时对话；
- DeepSeek 工具规划和最终回复；
- 工具：music、rec、weather、time、location；
- 前端可点击 AI 推荐歌曲并交给 MusicPanel 播放。

## 2. 必要服务

| 服务 | 端口 | 用途 |
|:--|:--:|:--|
| Gateway | 8080 | 前端统一访问 |
| module-chat | 8081 | 对话服务 |
| module-music | 8082 | 歌曲搜索、网易云搜索、候选池 |
| module-rec | 8083 | 每日推荐、热门榜、偏好 |
| MySQL | 3306 | `mall.chat_history` |
| Nacos | 8848 | 服务发现 |
| DeepSeek | 外部 | AI 工具规划和回复，可选 |
| QWeather | 外部 | 真实天气，可选 |

## 3. 环境变量

| 变量 | 必填 | 说明 |
|:--|:--:|:--|
| `MYSQL_PASSWORD` | 是 | MySQL root 密码 |
| `DEEPSEEK_API_KEY` | 否 | 配置后启用真实 AI |
| `DEEPSEEK_API_URL` | 否 | 默认 `https://api.deepseek.com/v1/chat/completions` |
| `DEEPSEEK_MODEL` | 否 | 默认 `deepseek-chat` |
| `QWEATHER_API_KEY` | 否 | 配置后启用真实天气 |
| `QWEATHER_API_HOST` | 建议 | 和风天气专属 API Host |
| `QWEATHER_WEATHER_URL` | 否 | 覆盖天气接口 |
| `QWEATHER_GEO_URL` | 否 | 覆盖城市查询接口 |

不要把真实 key 写进 Git。

## 4. AI 工具流程

```text
用户输入
  -> DeepSeekChatClient.planToolUse
  -> 白名单校验工具
  -> ChatService.executeTools
  -> DeepSeekChatClient.composeReply
  -> ChatSendResponse
```

白名单：

```text
music.search
music.catalog
music.neteaseSearch
rec.daily
rec.hot
rec.preferences
location.current
time.current
weather.now
```

当前没有任意联网搜索工具。

## 5. 浏览器定位工具

如果用户说“我这里天气怎么样”“我现在在哪”，且后端没有位置上下文，WebSocket 会返回：

```json
{
  "type": "tool_request",
  "clientToolRequests": [
    { "id": "location-current", "name": "location.current", "purpose": "获取浏览器当前位置" }
  ]
}
```

前端调用浏览器 `navigator.geolocation`，再带 `context.location` 重发原消息。

## 6. 天气接口

`GET /chat/weather?city=北京` 或 `GET /chat/weather?city=122.1,37.5`

真实天气返回 `source=real`；未配置 key、host 错误、城市失败、网络失败时返回 `source=demo`，并在 `message` 写明原因。

## 7. 历史记录

优先写 MySQL `mall.chat_history`。数据库不可用时写内存缓存，30 秒后重试数据库。

建表脚本：

```text
module-chat/src/main/resources/sql/chat_history.sql
```

## 8. 验证命令

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/chat/hello"
Invoke-RestMethod "http://127.0.0.1:8080/chat/weather?city=北京"
Invoke-RestMethod -Method Post "http://127.0.0.1:8080/chat/send" `
  -ContentType "application/json" `
  -Body '{"userId":1,"content":"下雨天推荐几首歌","context":{}}'
```
