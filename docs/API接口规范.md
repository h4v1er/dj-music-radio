# API 接口规范

> 所有接口通过 Gateway (:8080) 统一入口，前端请求 `/api/xxx` → Vite 代理 → Gateway 路由

## 通用约定

- 基础路径：`http://localhost:8080`
- 请求格式：`application/json`
- 响应格式：`{ "code": 200, "msg": "success", "data": {} }`
- 分页格式：`{ "page": 1, "size": 10, "total": 100, "records": [] }`

---

## 一、对话服务 `/chat`（队员A :8081）

| 方法 | 路径 | 说明 | 状态 |
|:-----|:-----|:-----|:----:|
| `GET` | `/chat/hello` | 服务健康检查 | ✅ |
| `WS` | `/chat/ws` | WebSocket 实时对话 | ✅ |
| `POST` | `/chat/send` | 发送对话消息 | ✅ |
| `GET` | `/chat/history?userId=` | 获取对话历史 | ✅ |
| `GET` | `/chat/weather?city=` | 查询天气 | ✅ |

### WebSocket 消息格式

```json
// 客户端 → 服务端
{
  "type": "message",
  "userId": 1,
  "content": "根据我这里天气推荐几首歌",
  "context": {
    "location": {
      "city": "威海市",
      "latitude": 37.5,
      "longitude": 122.1,
      "source": "browser_geolocation"
    }
  }
}

// 服务端 → 客户端
{
  "type": "reply",
  "content": "好的，为你推荐以下歌曲...",
  "songs": [...],
  "selectedSongs": [...]
}

// 服务端 → 客户端：请求前端执行客户端工具
{
  "type": "tool_request",
  "content": "我现在在哪啊",
  "clientToolRequests": [
    {
      "id": "location-current",
      "name": "location.current",
      "purpose": "获取浏览器当前位置"
    }
  ]
}
```

### 发送对话消息

`POST /chat/send`

```json
{
  "userId": 1,
  "content": "我这里天气怎么样",
  "context": {
    "location": {
      "city": "威海市",
      "latitude": 37.5,
      "longitude": 122.1,
      "source": "browser_geolocation"
    }
  }
}
```

`context.location` 为可选上下文。没有该上下文但 AI 规划需要当前位置时，WebSocket 会返回 `tool_request(location.current)`，由前端执行浏览器定位后带着工具结果再次发送原消息。

### 对话可用工具

| 工具 | 执行端 | 说明 |
|:---|:---|:---|
| `location.current` | 前端 | 浏览器定位，需用户授权 |
| `time.current` | 后端 | 当前日期、时间、星期、时区 |
| `weather.now` | 后端 | 和风天气实时天气，包含温度、体感、湿度、风、降水、气压、能见度等 |
| `music.catalog` / `music.search` / `music.neteaseSearch` | 后端 | 歌曲候选、关键词搜索、网易云搜索 |
| `rec.daily` / `rec.hot` / `rec.preferences` | 后端 | 推荐模块数据 |

### 天气响应格式

`GET /chat/weather?city=北京`

```json
{
  "city": "北京",
  "icon": "☀️",
  "temp": "28°",
  "text": "晴",
  "greeting": "下午好，想听点什么？",
  "source": "real",
  "obsTime": "2026-07-17T14:00+08:00",
  "message": "和风天气实时数据",
  "feelsLike": "27°",
  "windDir": "东风",
  "windScale": "3",
  "windSpeed": "13",
  "humidity": "94",
  "precip": "0.0",
  "pressure": "996",
  "vis": "7",
  "cloud": "100",
  "dew": "23",
  "updateTime": "2026-07-17T14:03+08:00",
  "fxLink": "https://www.qweather.com/..."
}
```

`source=real` 表示已调用和风天气真实接口；`source=demo` 表示未配置 key、host 错误、接口异常等情况下的显式演示降级，具体原因看 `message`。

---

## 二、音乐服务 `/music`（队员B :8082）

| 方法 | 路径 | 说明 | 状态 |
|:-----|:-----|:-----|:----:|
| `GET` | `/music/hello` | 服务健康检查 | ✅ |
| `GET` | `/music/song/list` | 歌曲列表（分页） | 📌 TODO |
| `GET` | `/music/song/{id}` | 歌曲详情 | 📌 TODO |
| `GET` | `/music/song/search?kw=` | 搜索歌曲 | 📌 TODO |
| `GET` | `/music/playlist/list?userId=` | 用户歌单列表 | 📌 TODO |
| `POST` | `/music/playlist/create` | 创建歌单 | 📌 TODO |
| `POST` | `/music/playlist/import` | 导入歌单（异步，RabbitMQ） | 📌 TODO |
| `POST` | `/music/playlist/{id}/addSong` | 添加歌曲到歌单 | 📌 TODO |

### 歌曲对象

```json
{
  "id": 1,
  "title": "Bohemian Rhapsody",
  "artist": "Queen",
  "album": "A Night at the Opera",
  "coverUrl": "...",
  "audioUrl": "...",
  "duration": 354,
  "genre": "摇滚"
}
```

---

## 三、推荐服务 `/rec`（队员C :8083）

| 方法 | 路径 | 说明 | 状态 |
|:-----|:-----|:-----|:----:|
| `GET` | `/rec/hello` | 服务健康检查 | ✅ |
| `GET` | `/rec/daily?userId=` | 今日推荐 | 📌 TODO |
| `GET` | `/rec/hot` | 热门榜单（Redis ZSET） | 📌 TODO |
| `GET` | `/rec/similar?songId=` | 相似歌曲推荐 | 📌 TODO |
| `POST` | `/rec/behavior` | 上报用户行为 | 📌 TODO |

### 用户行为上报

```json
{
  "userId": 1,
  "songId": 10,
  "action": "play"  
}
// action 可选值: play(播放) | like(收藏) | skip(跳过) | share(分享)
```

---

## 四、用户服务 `/user`（队员D :8084）

| 方法 | 路径 | 说明 | 状态 |
|:-----|:-----|:-----|:----:|
| `GET` | `/user/hello` | 服务健康检查 | ✅ |
| `POST` | `/user/register` | 注册 | 📌 TODO |
| `POST` | `/user/login` | 登录（返回JWT） | 📌 TODO |
| `GET` | `/user/info` | 获取个人信息 | 📌 TODO |
| `PUT` | `/user/info` | 修改个人信息 | 📌 TODO |
| `POST` | `/user/favorite/add` | 收藏歌曲 | 📌 TODO |
| `DELETE` | `/user/favorite/{songId}` | 取消收藏 | 📌 TODO |
| `GET` | `/user/history` | 播放历史 | 📌 TODO |

### 登录响应

```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1,
    "username": "user1"
  }
}
```

---

## OpenFeign 内部调用

| 调用方 | 被调用方 | 接口 | 用途 |
|:---|:---|:-----|:-----|
| module-chat | module-music | `GET /music/song/search?kw=` | 对话中搜索歌曲 |
| module-chat | module-rec | `GET /rec/daily?userId=` | 对话中获取推荐 |
| module-rec | module-user | `GET /user/info` | 推荐时获取用户偏好 |
| module-music | module-rec | `POST /rec/behavior` | 播放时上报行为 |
