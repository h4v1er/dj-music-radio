# module-music 运行与实现说明

负责范围：队员 B 音乐中心、播放器、歌单、网易云、歌词、情绪分析、收藏、历史。

## 1. 当前能力

- 本地歌曲分页、搜索、详情、流派；
- 歌单 CRUD、歌单歌曲排序；
- 网易云搜索、播放 URL、歌词、详情、歌单、封面代理；
- 网易云/JSON 歌单导入；
- RabbitMQ 异步导入；
- 歌词保存；
- DeepSeek 或关键词情绪分析；
- 用户品味画像；
- 收藏和播放历史。

## 2. 必要依赖

| 依赖 | 用途 |
|:--|:--|
| MySQL `dj_music_radio` | 歌曲、歌单、收藏、历史、情绪、品味 |
| RabbitMQ | 歌单导入异步队列 |
| Nacos | 服务发现 |
| NeteaseCloudMusicApi 3000 | 网易云搜索/播放/歌词/歌单 |
| DeepSeek API Key | 歌词情绪 AI 分析，可选 |

## 3. 环境变量

| 变量 | 说明 |
|:--|:--|
| `MYSQL_PASSWORD` | MySQL root 密码 |
| `RABBITMQ_USERNAME` | 默认 `guest` |
| `RABBITMQ_PASSWORD` | 默认 `guest` |
| `DEEPSEEK_API_KEY` | 配置后启用 AI 歌词情绪分析 |
| `DEEPSEEK_API_URL` | 默认 DeepSeek OpenAI 兼容接口 |
| `DEEPSEEK_MODEL` | 默认 `deepseek-chat` |

## 4. 数据库初始化

执行顺序：

```text
module-music/src/main/resources/init.sql
module-music/src/main/resources/emotion_schema.sql
module-music/src/main/resources/emotion_keywords.sql
```

注意：`init.sql` 会 `DROP TABLE` 并重建 music 主表，已有数据会被清掉。真实演示环境不要随便重复执行。

## 5. 网易云 API 代理

启动：

```powershell
cd D:\projects\dj-music-radio
powershell -ExecutionPolicy Bypass -File scripts\netease\start-netease-api.ps1
```

检查：

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/music/netease/ping"
Invoke-RestMethod "http://127.0.0.1:8080/music/netease/search?keywords=周杰伦&limit=3"
```

VIP、版权限制、地区限制导致播放 URL 为空属于外部服务正常限制。

## 6. 歌单导入

前端 `ImportDialog.vue` 支持：

- 粘贴网易云歌单链接或 ID；
- 手动输入 JSON 歌曲列表。

后端流程：

```text
POST /music/playlist/import
  -> RabbitTemplate 发送 music.exchange / playlist.import
  -> PlaylistImportListener 消费
  -> 写 playlist/song/playlist_song
  -> 获取歌词
  -> 提交情绪分析
  -> 更新用户品味
```

## 7. 情绪分析

优先 DeepSeek：

```text
DeepSeekClient.analyzeLyric
```

失败后回退：

```text
LyricsAnalyzer 关键词匹配
```

结果写入：

```text
song_emotion
song.emotion_tags
song.emotion_analyzed
user_taste
```

## 8. 验证命令

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/music/hello"
Invoke-RestMethod "http://127.0.0.1:8080/music/song/list?page=1&size=5"
Invoke-RestMethod "http://127.0.0.1:8080/music/song/genres"
Invoke-RestMethod "http://127.0.0.1:8080/music/netease/ping"
Invoke-RestMethod "http://127.0.0.1:8080/music/favorite/list?userId=1"
Invoke-RestMethod "http://127.0.0.1:8080/music/history/list?userId=1"
```
