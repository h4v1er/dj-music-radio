# module-rec 运行与实现说明

负责范围：队员 C 每日推荐、热门榜、相似推荐、用户行为和偏好标签。

## 1. 当前能力

- `GET /rec/hello` 健康检查；
- `GET /rec/hot` Redis 热门榜 TOP10；
- `POST /rec/behavior` 用户行为上报；
- `GET /rec/daily?userId=1` 今日推荐；
- `GET /rec/similar?songId=1` 相似歌曲；
- `GET /rec/preferences?userId=1` 用户偏好标签；
- `RecommendScheduler` 每天 02:00 生成推荐；
- RabbitMQ 发送推荐更新通知；
- OpenFeign 调用 `module-music` 补全歌曲信息。

## 2. 必要依赖

| 依赖 | 用途 |
|:--|:--|
| MySQL `mall` | `user_behavior`、`daily_recommend` |
| Redis | `song:hot:rank` 热门榜 |
| RabbitMQ | 推荐更新通知 |
| Nacos | 服务发现 |
| module-music | 歌曲详情和搜索 |

## 3. 环境变量

| 变量 | 说明 |
|:--|:--|
| `MYSQL_PASSWORD` | MySQL root 密码 |
| `RABBITMQ_USERNAME` | 默认 `guest` |
| `RABBITMQ_PASSWORD` | 默认 `guest` |

## 4. 数据表

新环境需要在 `mall` 库执行：

```text
module-rec/src/main/resources/sql/rec_tables.sql
```

脚本内容：

```sql
CREATE TABLE IF NOT EXISTS user_behavior (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  song_id INT NOT NULL,
  action VARCHAR(20) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_action (user_id, action),
  INDEX idx_song_action (song_id, action)
);

CREATE TABLE IF NOT EXISTS daily_recommend (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  song_id INT NOT NULL,
  reason VARCHAR(200),
  push_date DATE NOT NULL,
  INDEX idx_user_date (user_id, push_date)
);
```

## 5. 推荐逻辑

### 5.1 热门榜

行为上报时按 action 增加 Redis ZSET：

| 行为 | 分数 |
|:--|--:|
| `play` | 1 |
| `like` | 3 |
| `share` | 2 |
| `skip` | 0 |

`/rec/hot` 读取 `song:hot:rank` 并反向排序。

### 5.2 相似推荐

```text
输入 songId
  -> Feign 调 module-music 查询歌曲详情
  -> 优先同流派
  -> 不足时补同歌手
  -> 仍不足时按用户行为协同过滤
```

### 5.3 每日推荐

定时任务每天 02:00：

```text
查有行为的用户
  -> 找用户 Top 行为歌曲
  -> 生成相似推荐
  -> 写 daily_recommend
  -> RabbitMQ 通知
```

## 6. 验证命令

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/rec/hello"
Invoke-RestMethod "http://127.0.0.1:8080/rec/hot"
Invoke-RestMethod -Method Post "http://127.0.0.1:8080/rec/behavior" `
  -ContentType "application/json" `
  -Body '{"userId":1,"songId":1,"action":"play"}'
Invoke-RestMethod "http://127.0.0.1:8080/rec/preferences?userId=1"
Invoke-RestMethod "http://127.0.0.1:8080/rec/similar?songId=1"
```
