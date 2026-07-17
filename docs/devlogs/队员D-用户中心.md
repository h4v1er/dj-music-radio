# 队员D 开发日志 — 用户中心 + 基础设施模块

> 姓名：______  分支：`dev-user`  后端：`module-user` (:8084)

## 负责内容

- 用户注册、登录、JWT；
- 用户资料；
- 用户接口 JWT 鉴权；
- 用户收藏和播放历史归属统一；
- 底部用户状态栏；
- 基础设施联调。

## 当前仓库状态

- [x] `module-user` 启动类已存在；
- [x] `/user/hello` 健康检查已存在；
- [x] `application.yml` 已配置端口 8084、Nacos、MySQL、Redis、MyBatis Plus；
- [x] `UserBar.vue` 已有底部用户状态栏和登录弹窗 UI；
- [x] `frontend/src/api/user.js` 已接注册、登录、用户信息、收藏、历史接口；
- [x] 后端注册接口已实现；
- [x] 后端登录接口已实现；
- [x] JWT 签发和 user 接口校验已实现；
- [x] 真实用户资料接口已实现；
- [x] 收藏和播放历史与 music 模块的边界已统一；
- [ ] Gateway 全局鉴权过滤器未实现，当前不是全站强制登录。

## 当前整合结论

`module-music` 已经实现 `user_favorite` 和 `play_history`，本次整合选择继续由 music 模块作为收藏/历史的唯一数据源。`module-user` 只负责校验 JWT 和确定当前用户 ID，然后通过 OpenFeign 调用 music 接口。

这样播放器、推荐、用户中心看到的是同一套收藏和播放历史，不会出现 user 和 music 两边数据不一致。

## 后续增强

- Gateway 统一鉴权过滤器；
- 注册验证码、找回密码或 token 刷新；
- 头像上传和更完整的用户资料页；
- 用户注销、修改密码等账号安全能力。
