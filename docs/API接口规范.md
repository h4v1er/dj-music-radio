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
| `WS` | `/chat/ws` | WebSocket 实时对话 | 📌 TODO |
| `POST` | `/chat/send` | 发送对话消息 | 📌 TODO |
| `GET` | `/chat/history?userId=` | 获取对话历史 | 📌 TODO |
| `GET` | `/chat/weather?city=` | 查询天气 | 📌 TODO |

### WebSocket 消息格式

```json
// 客户端 → 服务端
{ "type": "message", "content": "推荐一些摇滚歌曲" }

// 服务端 → 客户端
{ "type": "reply", "content": "好的，为你推荐以下歌曲...", "songs": [...] }
```

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
