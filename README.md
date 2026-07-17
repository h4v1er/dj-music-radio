# DJ 音乐电台

一个基于 Spring Cloud + Vue3 的单屏 DJ 音乐控制台，包含 AI 对话、天气时间、音乐播放、网易云代理、歌单导入、歌词情绪分析、热门榜和每日推荐。

## 当前状态

当前集成分支是 `dev-chat`。该分支已经整合：

- 队员 A：`module-chat` 智能对话、天气、时间、定位、跨模块工具；
- 队员 B：`module-music` 音乐中心、网易云、歌单、歌词、情绪、收藏历史；
- 队员 C：`module-rec` 推荐、热榜、每日推荐、偏好标签；
- 队员 D：`module-user` 当前仍是占位，只有 `/user/hello`。

## 先看文档

文档入口：

```text
docs/README.md
```

新人从零运行：

```text
docs/新人运行手册.md
```

答辩和 PPT 资料：

```text
docs/项目完整说明书.md
docs/架构设计.md
```

## 服务端口

| 服务 | 端口 |
|:--|:--:|
| Frontend | 5173 |
| Gateway | 8080 |
| module-chat | 8081 |
| module-music | 8082 |
| module-rec | 8083 |
| module-user | 8084 |
| NeteaseCloudMusicApi | 3000 |
| Nacos | 8848 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672 / 15672 |

## 快速运行

完整步骤以 [docs/新人运行手册.md](./docs/新人运行手册.md) 为准。当前后端父 POM 使用 Java 25，远端 Windows 构建请使用 JDK25 脚本。

```powershell
cd D:\projects\dj-music-radio
powershell -ExecutionPolicy Bypass -File D:\projects\mvn25-dj-music-radio.ps1 clean package -DskipTests
```

前端：

```powershell
cd D:\projects\dj-music-radio\frontend
npm.cmd install
npm.cmd run dev -- --host 0.0.0.0
```

不要提交真实 `DEEPSEEK_API_KEY`、`QWEATHER_API_KEY`、MySQL 密码或其他个人密钥。
