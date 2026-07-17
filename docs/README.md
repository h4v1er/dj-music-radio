# DJ 音乐电台文档中心

仓库：`git@github.com:h4v1er/dj-music-radio.git`
当前集成分支：`dev-chat`
当前基准：已完成 `feat(chat): 扩展天气详情与时间工具`，并已整合 `music`、`rec` 模块阶段性最新实现。`user` 模块仍处于基础占位状态，后续整合完成后需要再复核一轮。

## 先读哪几份

| 顺序 | 文档 | 用途 |
|:--|:--|:--|
| 1 | [新人运行手册.md](./新人运行手册.md) | 新人从 clone 到跑通全项目的操作步骤 |
| 2 | [项目完整说明书.md](./项目完整说明书.md) | 答辩/PPT/项目文档素材，按当前代码完整说明 |
| 3 | [API接口规范.md](./API接口规范.md) | 前后端联调、模块接口、响应结构 |
| 4 | [架构设计.md](./架构设计.md) | 微服务架构、技术选型、数据流和部署关系 |
| 5 | [组员开发手册.md](./组员开发手册.md) | 分支、提交、协作、合并规范 |

## 模块文档

| 模块 | 文档 | 当前状态 |
|:--|:--|:--|
| `module-chat` | [module-chat-runtime.md](./module-chat-runtime.md) | 已接 DeepSeek 工具规划、WebSocket、天气、时间、浏览器定位、music/rec 联动 |
| `module-music` | [module-music-runtime.md](./module-music-runtime.md) | 已接本地歌库、网易云代理、歌单导入、歌词、情绪分析、收藏/历史 |
| `module-rec` | [module-rec-runtime.md](./module-rec-runtime.md) | 已接 Redis 热榜、每日推荐、相似推荐、行为采集、RabbitMQ 通知 |
| `module-user` | [module-user-runtime.md](./module-user-runtime.md) | 当前只有 `/user/hello`，登录注册/JWT 等仍待队员 D 完成 |
| `frontend` | [../frontend/README.md](../frontend/README.md) | 单屏 DJ 控制台，三栏布局，聊天推荐可点击播放 |

## 当前真实服务清单

| 服务 | 端口 | 说明 |
|:--|:--:|:--|
| Gateway | 8080 | 统一入口，路由 `/chat/**` `/music/**` `/rec/**` `/user/**` |
| module-chat | 8081 | AI 对话、WebSocket、天气、历史 |
| module-music | 8082 | 音乐中心、网易云代理转发、歌单、情绪分析 |
| module-rec | 8083 | 热门榜、每日推荐、相似推荐、偏好标签 |
| module-user | 8084 | 当前健康检查占位 |
| Frontend | 5173 | Vite + Vue3 单页控制台 |
| NeteaseCloudMusicApi | 3000 | 外部 Node 服务，本仓库脚本安装到 `.runtime/` |
| Nacos | 8848 | 服务发现 |
| MySQL | 3306 | `mall`、`dj_music_radio` 两个库 |
| Redis | 6379 | 推荐热榜 |
| RabbitMQ | 5672 / 15672 | 歌单导入、推荐通知 |

## 当前文件结构

```text
dj-music-radio/
├── pom.xml
├── common/                 公共依赖模块
├── gateway/                Spring Cloud Gateway
├── module-chat/            队员A：智能对话 DJ
├── module-music/           队员B：音乐中心
├── module-rec/             队员C：每日推荐
├── module-user/            队员D：用户中心占位
├── frontend/               Vue3 + Vite 前端
├── scripts/netease/        网易云 API 代理启动脚本
├── docs/                   项目文档
└── src/main/java/...       旧商城模板代码，当前不属于父 POM 多模块主线
```

## 重要约束

- 当前远端 Windows 是正式开发运行环境，项目位于 `D:\projects\dj-music-radio`。
- Mac 上的 `dj-music-radio-sshfs` 只是远端正式仓库的挂载入口，用于读写文件；Git、构建、运行仍在远端执行。
- 不要把真实 `DEEPSEEK_API_KEY`、`QWEATHER_API_KEY`、MySQL 密码提交到仓库。
- 当前父 POM 的 `java.version` 是 `25`，远端默认 `java` 可能是 17；构建时应使用远端 JDK25 脚本或统一调整版本。
