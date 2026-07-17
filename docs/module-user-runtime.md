# module-user 运行与实现说明

负责范围：队员 D 用户中心。本文档只描述当前真实代码，不把预留接口写成已完成。

## 1. 当前能力

当前后端只实现：

```text
GET /user/hello
```

返回健康检查字符串。

## 2. 已有但未完成的部分

| 位置 | 当前状态 |
|:--|:--|
| `module-user/src/main/java/org/example/user/UserApplication.java` | Spring Boot 启动类 |
| `module-user/src/main/java/org/example/user/controller/UserController.java` | 只有 `/user/hello` |
| `module-user/src/main/resources/application.yml` | 配置了端口、Nacos、MySQL、Redis、MyBatis Plus |
| `frontend/src/components/UserBar.vue` | 有登录弹窗和状态栏 UI，但只是本地状态 |
| `frontend/src/api/user.js` | 预留 register/login/info/favorite/history API |

## 3. 不能误认为已完成的能力

当前没有：

- 注册；
- 登录；
- JWT；
- token 校验；
- 用户资料接口；
- 后端收藏接口；
- 后端播放历史接口；
- Gateway 鉴权过滤器。

music 模块已经有收藏和播放历史接口。user 模块后续实现时，需要和 music 的 `user_favorite`、`play_history` 做边界统一，避免出现两套收藏/历史数据。

## 4. 运行依赖

| 依赖 | 当前用途 |
|:--|:--|
| MySQL `mall` | 已配置，当前代码未实际建用户业务表 |
| Redis | 已配置，当前代码未实际使用 |
| Nacos | 服务注册发现 |

环境变量：

```powershell
[Environment]::SetEnvironmentVariable("MYSQL_PASSWORD", "你的MySQL密码", "User")
```

## 5. 验证命令

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/user/hello"
```

## 6. 后续整合建议

1. 明确用户表结构和 userId 类型；
2. 实现注册、登录、JWT 签发和校验；
3. 前端 Axios 请求统一注入 Authorization；
4. 改造 UserBar 使用真实用户信息；
5. 统一收藏、播放历史归属：复用 music 接口，或迁移到 user 后让 music 调 user；
6. 在 Gateway 增加需要登录接口的鉴权策略；
7. 合入后再次更新所有文档和答辩说明。
