# 📚 DJ 音乐电台 — 文档中心

> 仓库：https://github.com/h4v1er/dj-music-radio

---

## 新人必读（按顺序）

| # | 文档 | 读它干什么 |
|:--|:-----|:-----|
| 1 | [**项目总体设计.md**](./项目总体设计.md) | 🔥 最重要！项目到底做成什么样、每人交付什么 |
| 2 | [**组员开发手册.md**](./组员开发手册.md) | 怎么克隆、怎么建分支、怎么提交、常见错误 |

---

## 技术文档

| 文档 | 用途 |
|:-----|:-----|
| [架构设计.md](./架构设计.md) | 微服务架构图、技术选型、数据库表设计 → 答辩 PPT 引用 |
| [API接口规范.md](./API接口规范.md) | 4 个模块的接口约定 → 前后端联调依据 |
| [前端设计规范.md](./前端设计规范.md) | 颜色变量、面板样式、布局规则 → 保证界面风格统一 |
| [module-chat-runtime.md](./module-chat-runtime.md) | 智能对话 DJ 运行依赖：DeepSeek、和风天气、验证命令 |
| [module-music-runtime.md](./module-music-runtime.md) | 音乐中心额外依赖：网易云 API、DeepSeek、数据库初始化 |

---

## 环境速查

| 服务 | 端口 | 启动命令 |
|:-----|:----:|:---------|
| MySQL | 3306 | `brew services start mysql` |
| Redis | 6379 | `brew services start redis` |
| RabbitMQ | 5672 | `brew services start rabbitmq` |
| Nacos | 8848 | `bash ~/environment/nacos/bin/startup.sh -m standalone` |
| 全量安装 | — | `mvn install -DskipTests` |
| Gateway | 8080 | `mvn spring-boot:run -pl gateway` |
| module-chat | 8081 | `mvn spring-boot:run -pl module-chat` |
| module-music | 8082 | `mvn spring-boot:run -pl module-music` |
| module-rec | 8083 | `mvn spring-boot:run -pl module-rec` |
| module-user | 8084 | `mvn spring-boot:run -pl module-user` |
| VUE3 前端 | 5173 | `cd frontend && npm run dev` |
| 网易云 API 代理 | 3000 | `powershell -ExecutionPolicy Bypass -File scripts/netease/start-netease-api.ps1` |

> 详细配置步骤见 [组员开发手册.md](./组员开发手册.md) 第二步。

---

## 个人开发日志

| 队员 | 日志 | Git 分支 |
|:---|:-----|:-----|
| 队员A | [队员A-智能对话DJ.md](./devlogs/队员A-智能对话DJ.md) | `dev-chat` |
| 队员B | [队员B-音乐中心.md](./devlogs/队员B-音乐中心.md) | `dev-music` |
| 队员C | [队员C-每日推荐.md](./devlogs/队员C-每日推荐.md) | `dev-rec` |
| 队员D | [队员D-用户中心.md](./devlogs/队员D-用户中心.md) | `dev-user` |
| 参考 | [模板-开发日志.md](./devlogs/模板-开发日志.md) | — |

---

## 文件结构

```
dj-music-radio/
├── pom.xml                  ← 父 POM (Spring Boot 3.4.5 + Cloud 2024.0.1)
├── common/                  ← 公共模块
├── gateway/                 ← API 网关 :8080
├── module-chat/             ← 队员A 微服务 :8081
├── module-music/            ← 队员B 微服务 :8082
├── module-rec/              ← 队员C 微服务 :8083
├── module-user/             ← 队员D 微服务 :8084
├── frontend/                ← VUE3 前端 :5173
└── docs/                    ← 📚 文档（你在这里）
```
