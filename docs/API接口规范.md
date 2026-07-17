# API 接口规范

默认前端通过 Vite 代理访问：

- `/api/**` -> `http://localhost:8080/**` -> Gateway；
- `/music/**` -> `http://localhost:8082/music/**`，音乐模块开发阶段直连。

统一响应并未完全一致：

- `module-music` 使用 `Result<T>`：`{ "code": 200, "message": "success", "data": ... }`；
- `module-chat`、`module-rec` 多数接口直接返回对象、数组或字符串；
- `module-user` 使用 `{ "code": 200, "message": "success", "data": ... }`；
- 文档下方按真实代码分别说明。

## 1. Gateway

| 路径 | 转发目标 |
|:--|:--|
| `/chat/**` | `lb://module-chat` |
| `/music/**` | `lb://module-music` |
| `/rec/**` | `lb://module-rec` |
| `/user/**` | `lb://module-user` |

## 2. module-chat

### 2.1 接口清单

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/chat/hello` | 健康检查，返回字符串 |
| `POST` | `/chat/send` | 发送聊天消息 |
| `GET` | `/chat/history?userId=1` | 最近 10 条聊天历史 |
| `GET` | `/chat/weather?city=北京` | 天气查询，city 可为城市名或 `lon,lat` |
| `WS` | `/chat/ws` | WebSocket 实时聊天 |

### 2.2 `POST /chat/send`

请求：

```json
{
  "userId": 1,
  "content": "我这里下雨，推荐几首适合晚上听的歌",
  "context": {
    "location": {
      "city": "山东省 威海",
      "latitude": 37.5,
      "longitude": 122.1,
      "source": "browser_geolocation"
    }
  }
}
```

响应：

```json
{
  "reply": {
    "role": "dj",
    "text": "结合你这里的天气，我会偏向安静、柔和一点的氛围。",
    "time": "21:30"
  },
  "songs": ["Rain - Ryuichi Sakamoto"],
  "selectedSongs": [
    {
      "id": 14,
      "songId": 14,
      "sourceId": "14",
      "source": "PROJECT_CATALOG",
      "title": "Rain",
      "artist": "Ryuichi Sakamoto",
      "album": "Music For Film",
      "genre": "网易云",
      "coverUrl": "",
      "filePath": "",
      "duration": 0,
      "netease": false,
      "playable": true
    }
  ],
  "toolCalls": [
    {
      "name": "weather.now",
      "purpose": "获取当前天气",
      "status": "ok",
      "summary": "天气：山东省 威海 多云 27°，source=real",
      "songCount": 0
    }
  ],
  "clientToolRequests": []
}
```

如果 AI 规划需要浏览器定位但请求没有 `context.location`：

```json
{
  "reply": { "role": "tool", "text": "", "time": "21:30" },
  "songs": [],
  "selectedSongs": [],
  "toolCalls": [],
  "clientToolRequests": [
    {
      "id": "location-current",
      "name": "location.current",
      "purpose": "获取浏览器当前位置"
    }
  ]
}
```

### 2.3 WebSocket

客户端发送：

```json
{
  "type": "message",
  "userId": 1,
  "content": "我现在在哪，天气怎么样",
  "context": {}
}
```

服务端普通回复：

```json
{
  "type": "reply",
  "content": "我查到你所在城市的天气...",
  "songs": [],
  "selectedSongs": [],
  "time": "21:30",
  "clientToolRequests": []
}
```

服务端请求前端工具：

```json
{
  "type": "tool_request",
  "content": "我现在在哪，天气怎么样",
  "songs": [],
  "selectedSongs": [],
  "time": null,
  "clientToolRequests": [
    {
      "id": "location-current",
      "name": "location.current",
      "purpose": "获取浏览器当前位置"
    }
  ]
}
```

### 2.4 天气响应

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

`source=demo` 表示真实天气不可用，`message` 会说明原因。

## 3. module-music

所有接口基础路径：`/music`。成功响应一般为：

```json
{ "code": 200, "message": "success", "data": {} }
```

### 3.1 歌曲

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/music/hello` | 健康检查 |
| `GET` | `/music/song/list?page=1&size=20&genre=` | 分页歌曲列表，按播放次数倒序 |
| `GET` | `/music/song/{id}` | 歌曲详情，同时播放次数 +1 |
| `GET` | `/music/song/search?kw=&page=1&size=20` | 按标题/歌手/专辑 LIKE 搜索 |
| `GET` | `/music/song/genres` | 全部流派 |
| `PUT` | `/music/song/{id}/lyric` | 保存歌词，body: `{ "lyric": "..." }` |
| `POST` | `/music/song/{id}/analyze-emotion` | 手动触发情绪分析 |

### 3.2 歌单

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/music/playlist/list?userId=1` | 用户歌单 |
| `GET` | `/music/playlist/{id}` | 歌单详情 |
| `GET` | `/music/playlist/{id}/songs` | 歌单歌曲 |
| `POST` | `/music/playlist` | 创建歌单 |
| `PUT` | `/music/playlist/{id}` | 更新歌单 |
| `DELETE` | `/music/playlist/{id}` | 删除歌单 |
| `POST` | `/music/playlist/{id}/song/{songId}` | 添加歌曲 |
| `DELETE` | `/music/playlist/{id}/song/{songId}` | 移除歌曲 |
| `PUT` | `/music/playlist/{id}/sort` | 歌曲排序，body: `{ "songIds": [1,2,3] }` |
| `POST` | `/music/playlist/import` | 异步导入歌单 |
| `GET` | `/music/playlist/import/status/{taskId}` | 当前简化返回 completed |

导入请求：

```json
{
  "name": "夜跑歌单",
  "userId": 1,
  "content": "[{\"title\":\"...\",\"artist\":\"...\",\"source\":\"NETEASE\",\"sourceId\":\"...\"}]"
}
```

### 3.3 网易云代理

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/music/netease/search?keywords=&limit=20` | 搜索歌曲 |
| `GET` | `/music/netease/url?id=` | 获取播放 URL，按多个音质降级尝试 |
| `GET` | `/music/netease/detail?ids=` | 歌曲详情 |
| `GET` | `/music/netease/lyric?id=` | 歌词 |
| `GET` | `/music/netease/lyric/batch?ids=1,2` | 批量歌词 |
| `GET` | `/music/netease/playlist?id=` | 歌单详情和歌曲列表 |
| `GET` | `/music/netease/cover?url=` | 封面代理 |
| `GET` | `/music/netease/ping` | 检查 3000 代理是否可用 |

### 3.4 情绪和品味

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/music/emotion/{songId}` | 获取歌曲情绪画像，不存在且有歌词时自动分析 |
| `POST` | `/music/emotion/analyze/{songId}` | 触发单首分析 |
| `POST` | `/music/emotion/batch/{playlistId}` | 异步分析歌单 |
| `GET` | `/music/emotion/search?tag=` | 按情绪标签找歌 |
| `GET` | `/music/emotion/playlist/{playlistId}/overview` | 歌单情绪总览 |
| `GET` | `/music/taste/{userId}` | 用户品味画像 |
| `GET` | `/music/taste/refresh/{userId}` | 重算用户品味 |

### 3.5 收藏和历史

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/music/favorite/list?userId=1` | 收藏列表 |
| `POST` | `/music/favorite/{songId}?userId=1` | 添加收藏 |
| `DELETE` | `/music/favorite/{songId}?userId=1` | 取消收藏 |
| `GET` | `/music/favorite/check/{songId}?userId=1` | 是否收藏 |
| `GET` | `/music/history/list?userId=1` | 最近播放历史 |
| `POST` | `/music/history` | 记录播放，body: `{ "userId": 1, "songId": 10 }` |

### 3.6 用户播放队列

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/music/queue/state?userId=1` | 查询用户当前播放队列、当前歌曲和播放模式 |
| `PUT` | `/music/queue/state` | 保存用户播放队列状态 |
| `DELETE` | `/music/queue/state?userId=1` | 清空用户播放队列状态 |

保存请求：

```json
{
  "userId": 1,
  "playMode": "order",
  "currentSong": {
    "id": 10,
    "title": "歌曲名",
    "artist": "歌手",
    "source": "NETEASE",
    "sourceId": "网易云歌曲ID"
  },
  "queue": [
    {
      "id": 10,
      "title": "歌曲名",
      "artist": "歌手",
      "source": "NETEASE",
      "sourceId": "网易云歌曲ID",
      "coverUrl": "",
      "filePath": "",
      "duration": 240
    }
  ]
}
```

## 4. module-rec

`module-rec` 当前直接返回数组或字符串。

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/rec/hello` | 健康检查 |
| `GET` | `/rec/hot` | Redis 热门榜 TOP10 |
| `GET` | `/rec/daily?userId=1` | 今日推荐 |
| `POST` | `/rec/daily/refresh?userId=1` | 根据最新行为手动刷新今日推荐 |
| `GET` | `/rec/similar?songId=1` | 相似歌曲 |
| `POST` | `/rec/behavior` | 上报用户行为 |
| `GET` | `/rec/preferences?userId=1` | 用户偏好标签 |

行为请求：

```json
{
  "userId": 1,
  "songId": 10,
  "action": "play"
}
```

`action` 可用：`play`、`like`、`skip`、`share`。

## 5. module-user

`module-user` 成功响应一般为：

```json
{ "code": 200, "message": "success", "data": {} }
```

| 方法 | 路径 | 说明 |
|:--|:--|:--|
| `GET` | `/user/hello` | 健康检查 |
| `POST` | `/user/register` | 注册用户 |
| `POST` | `/user/login` | 登录并返回 token |
| `GET` | `/user/info` | 查询当前用户，需要 `Authorization: Bearer <token>` |
| `PUT` | `/user/info` | 更新当前用户资料，需要 token |
| `PUT` | `/user/password` | 修改当前用户密码，需要 token，body: `{ "oldPassword": "...", "newPassword": "..." }` |
| `GET` | `/user/favorite/list` | 查询当前用户收藏，内部调用 music |
| `POST` | `/user/favorite/{songId}` | 添加收藏，内部调用 music |
| `POST` | `/user/favorite/add?songId=1` | 添加收藏兼容接口 |
| `DELETE` | `/user/favorite/{songId}` | 取消收藏，内部调用 music |
| `GET` | `/user/history` | 查询当前用户播放历史，内部调用 music |
| `POST` | `/user/history` | 记录播放历史，body: `{ "songId": 10 }` |
| `POST` | `/user/history/add?songId=10` | 记录播放历史兼容接口 |

注册请求：

```json
{
  "username": "demo_user",
  "password": "test123456",
  "nickname": "Demo",
  "phone": "13800000000",
  "email": "demo@example.com"
}
```

登录响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "JWT...",
    "userId": 1,
    "username": "demo_user",
    "nickname": "Demo",
    "avatar": ""
  }
}
```

说明：`module-user` 只保存用户身份资料；收藏和播放历史复用 `module-music` 的 `user_favorite`、`play_history`，避免两套数据冲突。

## 6. 前端跨组件事件

聊天推荐歌曲点击播放：

```js
window.dispatchEvent(new CustomEvent('dj-play-song', { detail: song }))
```

`MusicPanel.vue` 监听 `dj-play-song`，将聊天返回的 `selectedSongs` 转成播放器可识别歌曲对象；网易云歌曲会再请求播放 URL 和歌词。
