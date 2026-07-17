# module-user 运行与实现说明

负责范围：队员 D 用户中心、JWT 会话、用户资料，以及收藏/历史的用户入口。

## 1. 当前能力

- `GET /user/hello` 健康检查；
- `POST /user/register` 用户注册；
- `POST /user/login` 用户登录并返回 JWT；
- `GET /user/info` 根据 Authorization token 查询当前用户；
- `PUT /user/info` 更新当前用户昵称、头像、手机号、邮箱等资料；
- `GET /user/favorite/list` 查询当前用户收藏歌曲；
- `POST /user/favorite/{songId}` / `POST /user/favorite/add?songId=` 添加收藏；
- `DELETE /user/favorite/{songId}` 取消收藏；
- `GET /user/history` 查询当前用户播放历史；
- `POST /user/history` / `POST /user/history/add?songId=` 记录播放历史。

## 2. 实现边界

用户身份数据由 `module-user` 管理：

```text
mall.dj_user
```

收藏和播放历史不在 user 模块重复建表，统一复用 `module-music` 已有数据：

```text
dj_music_radio.user_favorite
dj_music_radio.play_history
```

`module-user` 通过 OpenFeign 调用 `module-music`，在校验 JWT 后把当前 `userId` 传给 music 模块。这样可以避免 user 和 music 各自维护一套收藏/历史数据。

## 3. 环境变量

| 变量 | 必填 | 说明 |
|:--|:--:|:--|
| `MYSQL_PASSWORD` | 是 | 连接 MySQL `mall` |
| `JWT_SECRET` | 否 | JWT HMAC 密钥，至少 32 字节；未配置时使用开发默认值 |
| `JWT_EXPIRE_MS` | 否 | token 过期时间，默认 86400000 |

## 4. 数据库初始化

在 `mall` 库执行：

```text
module-user/src/main/resources/sql/user_tables.sql
```

当前脚本创建：

```text
dj_user
```

密码保存为 BCrypt hash，不保存明文密码。

## 5. 依赖服务

| 依赖 | 用途 |
|:--|:--|
| MySQL `mall` | 用户表 |
| Nacos | 服务注册发现 |
| module-music | 收藏和播放历史 |
| MySQL `dj_music_radio` | music 模块收藏/历史数据 |

Redis 当前已在配置中保留，但本阶段还没有验证码或 session 存储逻辑。

## 6. 验证命令

注册：

```powershell
Invoke-RestMethod -Method Post "http://127.0.0.1:8080/user/register" `
  -ContentType "application/json" `
  -Body '{"username":"demo_user","password":"test123456","nickname":"Demo"}'
```

登录：

```powershell
$login = Invoke-RestMethod -Method Post "http://127.0.0.1:8080/user/login" `
  -ContentType "application/json" `
  -Body '{"username":"demo_user","password":"test123456"}'
$token = $login.data.token
```

查用户信息：

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/user/info" `
  -Headers @{ Authorization = "Bearer $token" }
```

查收藏和历史：

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/user/favorite/list" `
  -Headers @{ Authorization = "Bearer $token" }
Invoke-RestMethod "http://127.0.0.1:8080/user/history" `
  -Headers @{ Authorization = "Bearer $token" }
```

## 7. 前端联动

- `UserBar.vue` 负责真实登录、注册、退出、收藏和历史弹窗；
- 登录成功后 token 和用户信息写入 `localStorage`；
- 前端触发 `dj-user-session-changed` 事件；
- `ChatPanel.vue`、`MusicPanel.vue`、`RecPanel.vue` 会使用当前登录用户 ID 刷新历史、收藏、推荐和偏好；
- 收藏/历史弹窗中点击歌曲会触发 `dj-play-song`，交给 `MusicPanel.vue` 播放。

## 8. 后续可增强

- Gateway 统一鉴权过滤器；
- 注册验证码或找回密码；
- 用户头像上传；
- 用户资料页；
- 更细粒度的权限和 token 续期。
