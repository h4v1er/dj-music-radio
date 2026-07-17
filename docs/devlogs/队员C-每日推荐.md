# 队员C 开发日志 — 每日推荐模块

> 姓名：l1nky777  分支：`dev-rec`  后端：module-rec (:8083)

---

## 负责内容
- 📊 推荐面板（RecPanel.vue）
- 热门榜单（Redis ZSET）
- 个性化推荐算法
- 用户行为分析
- 每日推荐定时任务（@Scheduled）
- RabbitMQ 推荐推送

---

## 第1天（日期：2026-07-16）
- [x] 从 `master` 创建队员C开发分支 `dev-rec`
- [x] 搭建 module-rec 后端框架，完成 `GET /rec/hello` 服务健康检查
- [x] 完成 `GET /rec/hot` 热门榜单接口，Redis ZSET 反向排序返回 TOP10
- [x] 完成 `POST /rec/behavior` 用户行为上报接口，行为计分（play+1/like+3/share+2）写库同步更新 Redis
- [x] 完成 `GET /rec/similar?songId=` 相似歌曲推荐，当前整合版会结合情绪标签、有效流派、同歌手和行为协同过滤补足结果
- [x] 完成 `GET /rec/daily?userId=` 今日推荐查询接口
- [x] 完成 `GET /rec/preferences?userId=` 用户偏好标签接口，从行为数据统计流派频次
- [x] 新增 `MusicFeignClient` 通过 OpenFeign 调用 module-music，含 `MusicFeignFallback` 回退工厂
- [x] 新增 `SongDTO` / `ResultDTO` 数据传输对象，匹配 API 规范
- [x] 后端 enrich 热门榜单和每日推荐数据，Feign 补全歌曲标题歌手，失败自动降级
- [x] `UserBehaviorMapper` 新增 `findSimilarByBehavior` 协同过滤 SQL
- [x] 新增 `RecommendScheduler` 定时任务，`@Scheduled` 每天凌晨 2:00 生成当日推荐
- [x] 新增 RabbitMQ 配置 `RabbitMQConfig` + `RecNotificationProducer`，推荐生成后发送通知消息
- [x] `RecPanel.vue` 接入真实后端 API，热门榜单 / 今日推荐 / 偏好标签三态处理
- [x] 创建数据库表 `user_behavior` 和 `daily_recommend`
- [x] 安装并配置 Redis、Nacos、Erlang OTP 和 RabbitMQ，支撑推荐模块联调
- [x] 验证通过：Maven 编译成功，模块正常启动并注册到 Nacos，`/rec/hello` `/rec/hot` `/rec/preferences` 接口正常返回

## 第2天（日期：______）
- [x] `dev-rec` 阶段性代码已合入当前 `dev-chat` 集成分支
- [x] 与 `module-music` 联调：通过 Feign 查询歌曲详情和按关键词搜索歌曲
- [x] 前端 `RecPanel.vue` 已接入热门榜、今日推荐、偏好标签
- [x] 当前文档已补充 [module-rec-runtime.md](../module-rec-runtime.md)，记录运行依赖、数据表、推荐逻辑和验证命令
- [x] 补充正式 SQL 初始化脚本：`module-rec/src/main/resources/sql/rec_tables.sql`
- [x] 新增 `/rec/daily/refresh` 手动刷新接口，便于按当前用户最新播放/收藏行为重新生成今日推荐
- [x] 推荐策略从单一 genre 扩展为 emotionTags、有效 genre、artist、协同过滤、album、duration 多维度补足
- [x] 前端 `RecPanel.vue` 增加刷新按钮，推荐项可点击播放并联动 MusicPanel

## 第3天（日期：______）
- [ ] 

## 第4天（日期：______）
- [ ] 
