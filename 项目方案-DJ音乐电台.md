# 🎧 桌面式对话音乐 DJ 电台 — 项目实训方案

> 四人组 | 技术栈对齐度：**8.5/10** | 创新度：**高**

---

## 一、想法与老师技术栈贴合度评估

### ✅ 完美覆盖（必选全命中 + 可选能用上）

| 老师要求 | 本项目如何用上 | 贴合度 |
|:---------|:-------------|:-----:|
| **Spring Boot / Spring / Spring MVC** | 后端核心框架，构建 REST API | ★★★★★ |
| **MyBatis Plus** | 用户、歌单、音乐元数据 CRUD | ★★★★★ |
| **Redis** | 热门歌曲缓存、排行榜、验证码、用户行为Session | ★★★★★ |
| **RabbitMQ** | 歌单导入异步处理、每日推荐消息推送 | ★★★★☆ |
| **VUE3** | DJ电台主界面，对话面板，音乐可视化 | ★★★★★ |
| **Spring Cloud Nacos** | 微服务注册中心、配置中心 | ★★★★★ |
| **Spring Cloud Gateway** | 统一网关，路由转发，限流 | ★★★★★ |
| **Spring Cloud OpenFeign** | 微服务间远程调用（如推荐服务调用户服务） | ★★★★★ |
| **Node.js** | VUE3 构建工具链 (Vite)，或写 BFF 层 | ★★★★☆ |
| **Elasticsearch（可选）** | 音乐全文搜索（歌名/歌手/歌词） | ★★★★☆ |
| **Docker（可选）** | 一键部署，体现工程能力 | ★★★★☆ |

### ⚠️ 技术栈覆盖的唯一短板

| 技术 | 风险 | 解决方案 |
|:-----|:-----|:---------|
| **RabbitMQ** | 音乐场景下队列需求不突出 | 设计"歌单导入异步解析"、"每日推荐定时推送"、"用户行为日志异步收集"三个场景即可覆盖 |

### 📊 综合评分：**8.5 / 10**

> 结论：**非常合适！** 创意独特、技术覆盖面广、功能可拆分为 4 人工作量。

---

## 二、项目核心功能模块

### 模块划分（4 人，每人负责一个完整模块的前后端）

```
┌─────────────────────────────────────────────────────────┐
│                    对话式 DJ 音乐电台                      │
├──────────────┬──────────────┬──────────────┬────────────┤
│  模块一       │  模块二       │  模块三       │  模块四     │
│  智能对话DJ   │  音乐中心     │  每日推荐     │  用户中心    │
│  (模块负责人A) │  (模块负责人B) │  (模块负责人C) │  (模块负责人D)│
├──────────────┼──────────────┼──────────────┼────────────┤
│ • AI对话交互  │ • 歌单展示    │ • 每日推荐    │ • 登录注册   │
│ • 天气查询    │ • 音乐搜索    │ • 热门榜单    │ • 个人中心   │
│ • 语音指令    │ • 歌单导入    │ • 学习用户偏好 │ • 收藏管理   │
│ • DJ欢迎语    │ • 音乐播放器  │ • 定时推送    │ • 播放历史   │
└──────────────┴──────────────┴──────────────┴────────────┘
```

---

## 三、项目架构设计

### 3.1 微服务拆分

```
                              ┌──────────────┐
                              │   Nacos      │
                              │ 注册中心/配置 │
                              └──────┬───────┘
                                     │
   ┌─────────┐    ┌─────────┐    ┌───┴─────┐    ┌─────────┐
   │ VUE3    │───→│ Gateway │───→│ 路由分发 │───→│ 各微服务 │
   │ 前端     │    │ :8080   │    └─────────┘    └─────────┘
   └─────────┘    └─────────┘
                        │
          ┌─────────────┼─────────────┬──────────────┐
          ▼             ▼             ▼              ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
   │chat-svc  │  │music-svc │  │rec-svc   │  │user-svc  │
   │对话服务   │  │音乐服务   │  │推荐服务   │  │用户服务   │
   │:8081     │  │:8082     │  │:8083     │  │:8084     │
   └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
        │             │             │             │
        └─────────────┴──────┬──────┴─────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌─────────┐   ┌─────────┐   ┌──────────┐
        │  MySQL  │   │  Redis  │   │ RabbitMQ │
        │  :3306  │   │ :6379   │   │  :5672   │
        └─────────┘   └─────────┘   └──────────┘
```

### 3.2 各服务职责

| 微服务 | 端口 | 技术栈 | 核心职责 |
|:-------|:----:|:-------|:---------|
| `gateway` | 8080 | Spring Cloud Gateway | 统一入口、路由、限流、跨域 |
| `chat-service` | 8081 | Spring Boot + WebSocket | AI对话、天气查询、DJ互动 |
| `music-service` | 8082 | Spring Boot + ES | 歌曲搜索、歌单管理、播放记录 |
| `rec-service` | 8083 | Spring Boot + 定时任务 | 推荐算法、每日榜单、偏好学习 |
| `user-service` | 8084 | Spring Boot + JWT | 登录注册、个人信息、收藏管理 |

---

## 四、技术栈详细映射

### 4.1 Spring Cloud 微服务体系

| 组件 | 用途 | 具体场景 |
|:-----|:-----|:---------|
| **Nacos** | 注册中心 + 配置中心 | 4 个微服务注册发现；共享配置（天气API KEY等） |
| **Gateway** | API 网关 | 前端所有请求经网关路由到对应微服务；跨域配置 |
| **OpenFeign** | 服务间调用 | 推荐服务调用户服务拿用户收藏；对话服务调音乐服务搜索歌曲 |

### 4.2 中间件使用

| 中间件 | 场景 | 具体实现 |
|:-------|:-----|:---------|
| **Redis** | 缓存热门榜单 | `ZSET` 存储歌曲热度排行，每日凌晨刷新 |
| **Redis** | 用户行为暂存 | 记录用户听歌/收藏/跳过行为，用于偏好学习 |
| **Redis** | 验证码 | 登录验证码，5 分钟过期 |
| **RabbitMQ** | 歌单导入 | 用户上传歌单文件 → 发消息到队列 → 异步解析入库 |
| **RabbitMQ** | 推荐推送 | 定时任务扫描 → 生成推荐 → 发消息 → 推送通知 |
| **Elasticsearch** | 音乐全文搜索 | 歌名、歌手、歌词分词搜索 |

### 4.3 VUE3 前端技术

| 技术点 | 用途 |
|:-------|:-----|
| **VUE3 Composition API** | 组件开发 |
| **Element Plus** | UI 组件库 |
| **Pinia** | 状态管理（播放器状态、用户信息） |
| **WebSocket** | 实时对话 |
| **ECharts** | 音乐数据可视化 |
| **Howler.js** | 音乐播放引擎 |

---

## 五、数据库设计（核心表）

### 5.1 用户服务 (user-service)

```sql
-- 用户表
CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    avatar VARCHAR(255),
    nickname VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户收藏表
CREATE TABLE user_favorite (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    song_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 音乐服务 (music-service)

```sql
-- 歌曲表
CREATE TABLE song (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(100),
    album VARCHAR(100),
    cover_url VARCHAR(255),
    audio_url VARCHAR(255),
    duration INT COMMENT '秒',
    genre VARCHAR(50)
);

-- 歌单表
CREATE TABLE playlist (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    cover_url VARCHAR(255),
    is_public TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 歌单-歌曲关联
CREATE TABLE playlist_song (
    id INT PRIMARY KEY AUTO_INCREMENT,
    playlist_id INT NOT NULL,
    song_id INT NOT NULL,
    sort_order INT DEFAULT 0
);
```

### 5.3 对话服务 (chat-service)

```sql
-- 对话历史表
CREATE TABLE chat_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    role ENUM('user','assistant') NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 5.4 推荐服务 (rec-service)

```sql
-- 用户行为日志表
CREATE TABLE user_behavior (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    song_id INT NOT NULL,
    action ENUM('play','like','skip','share') NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 每日推荐表
CREATE TABLE daily_recommend (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    song_id INT NOT NULL,
    reason VARCHAR(200),
    push_date DATE NOT NULL
);
```

---

## 六、API 接口设计（核心）

### 6.1 对话服务 `/chat`

| 方法 | 路径 | 说明 |
|:-----|:-----|:-----|
| `POST` | `/chat/send` | 发送对话消息 |
| `GET` | `/chat/history?userId=` | 获取对话历史 |
| `GET` | `/chat/weather?city=` | 查询天气 |
| `WS` | `/chat/ws` | WebSocket 实时对话 |

### 6.2 音乐服务 `/music`

| 方法 | 路径 | 说明 |
|:-----|:-----|:-----|
| `GET` | `/music/song/search?kw=` | 搜索歌曲 |
| `GET` | `/music/song/{id}` | 歌曲详情 |
| `GET` | `/music/playlist/list` | 歌单列表 |
| `POST` | `/music/playlist/import` | 导入歌单（异步） |
| `GET` | `/music/playlist/{id}/songs` | 歌单中歌曲 |

### 6.3 推荐服务 `/rec`

| 方法 | 路径 | 说明 |
|:-----|:-----|:-----|
| `GET` | `/rec/daily?userId=` | 今日推荐 |
| `GET` | `/rec/hot` | 热门榜单 |
| `GET` | `/rec/similar?songId=` | 相似歌曲 |
| `POST` | `/rec/behavior` | 上报用户行为 |

### 6.4 用户服务 `/user`

| 方法 | 路径 | 说明 |
|:-----|:-----|:-----|
| `POST` | `/user/register` | 注册 |
| `POST` | `/user/login` | 登录 |
| `GET` | `/user/info` | 个人信息 |
| `POST` | `/user/favorite/add` | 收藏歌曲 |
| `GET` | `/user/history` | 播放历史 |

---

## 七、四人分工方案

### 👤 成员 A：智能对话 DJ 模块（chat-service + VUE3 对话面板）

**为什么这个模块工作量够？**
- WebSocket 实时通信（技术难度中等偏上）
- 接入免费天气 API（如和风天气）
- 设计"DJ 欢迎语"（根据时间段不同：早上"早安，来点活力音乐？"，晚上"夜深了，来首舒缓的"）
- 对话规则引擎（不一定要接 AI 大模型，用关键词匹配 + 预设话术也能出效果）
- VUE3 聊天界面组件开发

### 👤 成员 B：音乐中心模块（music-service + VUE3 播放器）

**为什么这个模块工作量够？**
- 音乐搜索（Elasticsearch 加分项）
- 歌单 CRUD + 歌单导入（RabbitMQ 异步解析导入文件）
- **VUE3 音乐播放器**（进度条、音量、播放列表、歌词滚动 — 前端工作量最大的一块）
- 音乐可视化（波形/频谱动画）

### 👤 成员 C：每日推荐模块（rec-service + VUE3 推荐页）

**为什么这个模块工作量够？**
- 推荐算法（协同过滤 或 基于标签的规则推荐）
- Redis 热门榜单（ZSET 排序）
- 用户行为分析（听歌记录 → 偏好标签）
- RabbitMQ 定时推送推荐消息
- Spring `@Scheduled` 每日推荐定时任务生成
- VUE3 推荐页面 + 数据可视化

### 👤 成员 D：用户中心模块（user-service + Gateway + Nacos + VUE3 用户页）

**为什么这个模块工作量够？**
- JWT 登录注册（含 Redis 验证码）
- 用户收藏、播放历史管理
- **Nacos 配置中心搭建**（团队基础设施担当）
- **Gateway 网关搭建**（统一入口、跨域、路由）
- VUE3 用户中心页面 + 收藏管理页面

---

## 八、项目周期建议（假设 4 周）

| 阶段 | 时间 | 目标 |
|:-----|:-----|:-----|
| **第 1 周** | 基础搭建 | 每个人建好自己的微服务模块 + 数据库表 + 能跑通基础接口 |
| **第 2 周** | 核心功能 | 播放器能放歌、对话能聊天、推荐能出结果、用户能登录 |
| **第 3 周** | 联调打通 | Gateway 统一入口、OpenFeign 服务调用、前端联调 |
| **第 4 周** | 完善打磨 | 前端美化、PPT、演示准备、Docker 部署 |

---

## 九、音乐来源方案（关键问题）

由于无法搭建真正的音乐服务器，推荐以下方案：

| 方案 | 优点 | 缺点 |
|:-----|:-----|:-----|
| **A. Mock 本地数据** | 可控、稳定、演示不出意外 | 音乐数量有限 |
| **B. 调用网易云 API** | 歌曲丰富 | API 有风控，可能被封 |
| **C. 组合方案（推荐）** | 预设 20 首本地歌曲 + 接口展示搜索能力 | — |

> **建议**：用方案 C。提前准备好 20 首无版权的歌曲文件（或使用免费音效），数据库录入元数据。演示时播放本地歌曲稳定可靠，搜索功能展示 API 调用能力即可。

---

## 十、现有开源项目参考

虽然没有完全匹配的项目，但有两个值得参考：

| 项目 | 地址 | 可参考的点 |
|:-----|:-----|:-----------|
| **music-website** | https://github.com/Yin-Hongwei/music-website | 音乐播放器前端、歌单管理、VUE3 组件 |
| **spring-cloud-alibaba-demo** | https://github.com/RemainderTime/spring-cloud-alibaba-base-demo | 微服务脚手架、Nacos+Gateway+OpenFeign 配置 |

---

## 十一、与网上类似项目的差异化亮点

| 对比维度 | 网上常见音乐毕设 | 本项目 |
|:---------|:---------------|:-------|
| 架构 | 单体 Spring Boot | ✅ 微服务 Spring Cloud |
| 交互 | 普通网页点击 | ✅ 对话式 AI 交互 |
| 推荐 | 简单热门榜单 | ✅ 基于用户行为的个性化推荐 |
| 中间件 | MySQL 单一数据库 | ✅ Redis + RabbitMQ + ES |
| 创新点 | 无 | ✅ DJ电台场景 + 天气 + 时间段感知 |
| 前端 | 简单列表页 | ✅ 精美的 DJ 电台主题 UI |

---

> 📌 **下一步**：如果你对这个方案满意，我可以帮你初始化项目代码骨架（4 个微服务模块 + Gateway + 公共模块），直接在你的 IDEA 里跑起来。
