# 🎧 DJ 音乐电台 — 团队协作规范

> 仓库: https://github.com/h4v1er/dj-music-radio

## 一、分支策略

```
master                  ← 主分支（稳定版，用于答辩演示）
  ├── dev-chat          ← 队员A — 智能对话DJ模块
  ├── dev-music         ← 队员B — 音乐中心模块
  ├── dev-rec           ← 队员C — 每日推荐模块
  └── dev-user          ← 队员D — 用户中心模块
```

### Git 工作流

```bash
# 1. 克隆仓库
git clone https://github.com/h4v1er/dj-music-radio.git
cd dj-music-radio

# 2. 设置自己的 Git 身份（重要！提交截图才能证明工作量）
git config user.name "你的姓名拼音"
git config user.email "你的邮箱"

# 3. 创建自己的开发分支
git checkout -b dev-chat    # 队员A
git checkout -b dev-music   # 队员B
git checkout -b dev-rec     # 队员C
git checkout -b dev-user    # 队员D

# 4. 日常开发
git add .
git commit -m "feat(chat): 实现WebSocket实时对话"
git push origin dev-chat

# 5. 功能完成后，在 GitHub 提 Pull Request 合并到 master
```

### 提交消息规范

```
feat(模块): 新功能描述       # feat(chat): 实现天气查询
fix(模块): 修复描述          # fix(music): 修复播放进度条
docs: 文档更新               # docs: 更新API接口文档
style: 样式调整               # style: 统一面板圆角
```

---

## 二、文档规范

### 项目级文档（所有人共同维护，放 `docs/`）

| 文档 | 说明 |
|:-----|:-----|
| [架构设计.md](./架构设计.md) | 微服务架构、技术选型、数据库设计 |
| [API接口规范.md](./API接口规范.md) | 各模块接口约定，联调依据 |
| [前端设计规范.md](./前端设计规范.md) | 颜色/字体/组件规范，样式统一 |

### 个人开发日志（每人写自己的，放 `docs/devlogs/`）

- **每天写几条**：做了什么、遇到什么问题、怎么解决的
- **格式随意**：时间 + 内容即可
- **答辩用途**：日志 + Git 截图 = 双份工作量证明

---

## 三、环境速查

| 服务 | 端口 | 如何启动 |
|:-----|:----:|:---------|
| MySQL | 3306 | `brew services start mysql` |
| Redis | 6379 | `brew services start redis` |
| RabbitMQ | 5672 | `brew services start rabbitmq` |
| Nacos | 8848 | `bash ~/environment/nacos/bin/startup.sh -m standalone` |
| Gateway | 8080 | `mvn spring-boot:run -pl gateway` |
| module-chat | 8081 | `mvn spring-boot:run -pl module-chat` |
| module-music | 8082 | `mvn spring-boot:run -pl module-music` |
| module-rec | 8083 | `mvn spring-boot:run -pl module-rec` |
| module-user | 8084 | `mvn spring-boot:run -pl module-user` |
| VUE3 前端 | 5173 | `cd frontend && npm run dev` |

详细环境配置见 [环境配置报告](../环境配置报告.md)

---

## 四、文件结构总览

```
dj-music-radio/
├── pom.xml                 ← 父 POM（统一版本管理）
├── common/                 ← 公共模块
├── gateway/                ← API 网关 (:8080)
├── module-chat/            ← 队员A — 智能对话 (:8081)
├── module-music/           ← 队员B — 音乐中心 (:8082)
├── module-rec/             ← 队员C — 每日推荐 (:8083)
├── module-user/            ← 队员D — 用户中心 (:8084)
├── frontend/               ← VUE3 前端 (:5173)
│   └── src/components/
│       ├── ChatPanel.vue   ← 队员A
│       ├── WeatherWidget.vue ← 队员A
│       ├── MusicPanel.vue  ← 队员B
│       ├── RecPanel.vue    ← 队员C
│       └── UserBar.vue     ← 队员D
└── docs/                   ← 项目文档
    ├── README.md           ← 本文档
    ├── 架构设计.md
    ├── API接口规范.md
    ├── 前端设计规范.md
    └── devlogs/            ← 个人开发日志
```
