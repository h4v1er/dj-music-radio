# 队员B 开发日志 — 音乐中心模块

> 姓名：______  分支：`dev-music`  后端：module-music (:8082)  前端：MusicPanel.vue + 5 子组件

---

## 负责内容

- 🎵 音乐播放器（封面旋转、进度条拖拽、音量、顺序/随机/单曲循环）
- 🎭 AI 情绪分析系统（DeepSeek API + 关键词匹配双引擎）
- 📋 歌单管理（CRUD + 拖拽排序）
- 📥 歌单导入（网易云 URL 解析 + RabbitMQ 异步处理）
- ☁️ 网易云音乐集成（搜索、在线播放、歌词获取、封面代理）
- 🔍 歌曲搜索（关键词模糊匹配 + 流派筛选）
- ❤️ 收藏管理 + 🕐 播放历史
- 💾 MySQL 数据库设计（8 张表：song / playlist / playlist_song / user_favorite / play_history / song_emotion / emotion_keyword / user_taste）

---

## 第1天（日期：7月16日）

### 完成内容
- [x] 克隆仓库 `dj-music-radio`，创建 `dev-music` 开发分支
- [x] 搭建 module-music 后端项目（Spring Boot 3.4.5 + MyBatis Plus 3.5.9），端口 8082
- [x] 设计并建库 `dj_music_radio`，执行 `init.sql` 创建 5 张业务表
- [x] 开发 8 个 Entity + 8 个 Mapper + MyBatis XML
- [x] 开发 5 个 Service 接口 + 实现类
- [x] 开发 7 个 Controller，共计 20+ REST API 接口
- [x] 统一响应 DTO（Result.java）
- [x] 安装 common 模块，mvn 编译通过

### 遇到的问题
- 项目为空骨架启动报错，common 模块依赖未安装

### 解决方案
- 先 `mvn install -DskipTests -pl common`，再 `mvn spring-boot:run -pl module-music`

**Git 提交**: `50b4c07`, `3e96ea6`, `aaa81aa`

---

## 第2天（日期：7月16日）

### 完成内容
- [x] 网易云歌单链接导入功能（解析 `music.163.com/playlist?id=` → 调用 NeteaseCloudMusicApi 获取歌曲列表 → RabbitMQ 异步入库）
- [x] 网易云封面代理（绕过 Referer 防盗链，`/music/netease/cover?url=`）
- [x] 网易云歌曲搜索（搜索 → 批量获取详情补封面 → 点击播放）
- [x] 播放器核心 PlayerCore.vue（HTML5 Audio + 封面旋转动画 + 进度条拖拽 + 音量控制 + 播放模式切换）
- [x] 歌单面板 PlaylistPanel.vue（创建/删除/选择）
- [x] 歌曲列表 SongList.vue（搜索、流派筛选、右键菜单、拖拽排序、情绪徽章）
- [x] 歌词面板 LyricsPanel.vue（LRC 解析 + 同步滚动）
- [x] 导入对话框 ImportDialog.vue（粘贴网易云链接 / 手动 JSON 输入）
- [x] 前端 API 层 music.js（20+ 接口调用方法 + getCoverUrl 代理函数）

### 遇到的问题
- 网易云搜索结果没有封面图（搜索 API 不返回 picUrl）
- 网易云封面图片因 Referer 防盗链 403

### 解决方案
- 搜索结果出来后批量调用 `/song/detail` 接口补封面
- 后端新增封面代理接口，设置 `Referer: https://music.163.com/`

**Git 提交**: `50b4c07` (网易云歌单链接导入 + 封面/风格/播放修复)

---

## 第3天（日期：7月16日）

### 完成内容
- [x] **AI 情绪分析系统——DeepSeek 集成**
  - 创建 `DeepSeekClient.java`，调用 DeepSeek API（OpenAI 兼容接口）
  - 设计中文音乐情绪分析 Prompt（20 种情绪标签 + 效价/唤醒度/强度/主题/场景）
  - `EmotionAnalysisServiceImpl` 改为 AI 优先 → 关键词回退双引擎
- [x] 情绪词典体系
  - 20 种情绪 × 5 大家族分类（MusicEmotion.java）
  - 500+ 中文关键词种子数据（emotion_keywords.sql）
  - LyricsAnalyzer.java 纯 Java 关键词匹配引擎（含否定检测）
  - EmotionCalculator.java 情绪向量计算 & 品味描述生成
- [x] 情绪数据库
  - song_emotion 表（效价/唤醒度/强度/主题/场景/氛围标签）
  - emotion_keyword 表（500+ 关键词权重）
  - user_taste 表（用户品味画像）
- [x] 前端情绪展示
  - EmotionTag.vue（5 色系情绪徽章 + emoji）
  - EmotionInfo.vue（双维度画像面板 + 手动分析按钮）

### 遇到的问题
- ❌ **Bug**: 歌曲只能播放几十秒（网易云 `/song/url` 默认返回试听片段）
- ❌ **Bug**: 情绪标签形同虚设（导入时 `song.setLyric(null)` → 无歌词 → 无法分析）
- ❌ **Bug**: 搜索到的歌曲情绪面板永远显示"该歌曲暂无情绪数据"

### 解决方案
- 播放 URL 升级：改用 `/song/url/v1` 接口，多音质等级 fallback（lossless → exhigh → higher → standard）
- 导入时获取歌词：`PlaylistImportListener` 新增 `fetchLyric()` 方法，从网易云 API 拿歌词并存入数据库
- 播放时缓存歌词：`MusicPanel.vue` 播放歌曲后异步 `saveLyric()` 到后端
- 手动分析入口：`EmotionInfo.vue` 增加"🔬 分析情绪"按钮，调用 `POST /song/{id}/analyze-emotion`
- 新增接口：`PUT /song/{id}/lyric`（保存歌词）、`GET /netease/lyric/batch`（批量获取歌词）

**Git 提交**: `c15e03e` (音乐中心完整功能 + Bug修复 + DeepSeek AI情绪分析)

---

## 第4天（日期：7月16日）

### 完成内容
- [x] 修复 `analyze-emotion` 接口返回 stale emotionTags 的 bug（分析后重新查库）
- [x] 后端编译验证 → 启动 → API curl 测试全部通过
- [x] 数据库建表脚本执行（emotion_schema.sql + emotion_keywords.sql）
- [x] **端到端验证：DeepSeek AI 真实分析**
  - 测试歌曲：`生如夏花 - 朴树`
  - 结果：主情绪「热烈奔放」、次情绪「触动感怀」、效价 +40、唤醒度 70
- [x] Git 提交 & Push → 创建 PR #1 合并到 master
- [x] 修复数据库 `song` 表缺少 `emotion_tags`/`emotion_analyzed` 列的兼容问题

### 遇到的问题
- 编译后启动 `/music/song/list` 报 500 错误
- `analyze-emotion` API 返回的 `emotionTags` 为空字符串

### 解决方案
- 数据库缺少情绪字段 → 执行 `ALTER TABLE song ADD COLUMN emotion_tags...`
- Controller 中 song 对象是分析前的旧值 → 分析后重新 `songMapper.selectById(id)` 获取最新数据

**Git 提交**: `80167dd` (修复analyze-emotion + 数据库兼容)

---

## 技术亮点总结

| 亮点 | 说明 |
|------|------|
| **DeepSeek AI 情绪分析** | 20 种中文情绪标签精准分类，效价/唤醒度双维度画像 |
| **双引擎设计** | AI 优先 → 关键词匹配回退，未配 API Key 也能用 |
| **异步导入** | 网易云 URL → RabbitMQ 异步解析 → 批量获取歌词 → 自动情绪分析 |
| **封面代理** | 绕过网易云 Referer 防盗链，后端代理转发 |
| **多音质 fallback** | lossless → exhigh → higher → standard → 原始接口，尽可能获取完整歌曲 |

---

## 文件统计

| 类别 | 数量 |
|------|------|
| Java 后端文件 | 43 个 |
| Vue3 前端组件 | 6 个（主面板 + 5 子组件） |
| SQL 脚本 | 3 个 |
| REST API 接口 | 24 个 |
| Git 提交 | 5 commits |
| 代码行数 | +3,600+ |

---

## 队员需自行安装的依赖

| 依赖 | 说明 |
|------|------|
| **NeteaseCloudMusicApi** `:3000` | `git clone` 后 `node app.js`，网易云搜索/播放/歌词/导入都依赖它 |
| **DeepSeek API Key** | platform.deepseek.com 免费注册，不配则回退关键词匹配 |
| **MySQL 数据库** | 执行 `init.sql` + `emotion_schema.sql` + `emotion_keywords.sql` |
