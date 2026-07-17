# 队员D 开发日志 — 用户中心 + 基础设施模块

> 姓名：______  分支：`dev-user`  后端：`module-user` (:8084)

## 负责内容

- 用户注册、登录、JWT；
- 用户资料；
- Gateway 鉴权；
- 用户收藏和播放历史归属统一；
- 底部用户状态栏；
- 基础设施联调。

## 当前仓库状态

- [x] `module-user` 启动类已存在；
- [x] `/user/hello` 健康检查已存在；
- [x] `application.yml` 已配置端口 8084、Nacos、MySQL、Redis、MyBatis Plus；
- [x] `UserBar.vue` 已有底部用户状态栏和登录弹窗 UI；
- [x] `frontend/src/api/user.js` 已预留注册、登录、用户信息、收藏、历史接口；
- [ ] 后端注册接口未实现；
- [ ] 后端登录接口未实现；
- [ ] JWT 签发和校验未实现；
- [ ] Gateway 鉴权未实现；
- [ ] 真实用户资料接口未实现；
- [ ] 收藏和播放历史与 music 模块的边界尚未统一。

## 后续整合提醒

`module-music` 当前已经实现 `user_favorite` 和 `play_history`。队员 D 合入用户模块时，需要先确定：

- 收藏和历史继续由 music 模块管理，user 只提供身份；
- 还是收藏和历史迁移到 user，由 music 调用 user。

不要同时保留两套写入逻辑，否则推荐、播放器和用户中心会看到不同数据。
