# AUTH-ROLE-1 实施计划与批准契约

用户已批准 Session + BCrypt + CSRF；仅本阶段认证授权，无 Git 写操作。
保留 MONTHLY-SYNTHETIC-DATA-1 完成事实、历史归档、气象 Schema 字段与 data.sql / tools/data。

## 统一契约

- POST /api/auth/login：JSON username/password/loginType；成功 data 为 id/username/role。
- POST /api/auth/logout：使 Session 失效，成功 code 200。
- GET /api/auth/me：同安全用户字段，未认证 401。
- GET /api/auth/csrf：data 为 token/headerName/parameterName；Cookie XSRF-TOKEN，头 X-XSRF-TOKEN。
- 错误密码、不存在、disabled 401；正确凭据但入口角色不符 403，不建立认证 Session。角色只来自 sys_user。
- USER 可读三字典、三展示查询；ADMIN 另可管理四资源。未认证 401；已认证越权或缺 CSRF 403。
- Session HttpOnly、fixation 防护、密码不留 Session。登录/退出也要求 CSRF；无缓存身份、无 JWT。
- 两个公开课程演示账户 demo_user / DemoUser@2026、demo_admin / DemoAdmin@2026，DEMO ONLY / NOT FOR PRODUCTION；不是数据库连接密码。
- sys_user 单独认证表；schema → data → auth-data。气象基线 16/2/6/46080，认证账户 2。

## Task 1: 后端与认证表

- [ ] 先编写并运行失败的认证/方法权限/CSRF/Session 测试。
- [ ] backend/pom.xml 增加 Boot 管理的 security 与 security-test；不升级业务依赖。
- [ ] SysUser、SysUserMapper、UserDetailsService、简单安全配置及 JSON 登录认证链，复用 ApiResponse。
- [ ] Safe user DTO、csrf/me 接口，普通成功/异常均 JSON；保留十九个业务接口语义。
- [ ] 明确方法+路径授权，默认拒绝；portable 静态页增加 /login。
- [ ] 原 Controller 测试加入实际安全配置、适当身份/CSRF，不能禁过滤器或删测试。
- [ ] schema 仅增加 sys_user；auth-data.sql 仅公开演示账户 BCrypt；真实集成验证留主流程新隔离库。
- [ ] 执行非数据库测试、test-compile，记录真实 RED/GREEN 与全部实际修改路径。

## Task 2: 前端会话与入口

- [ ] 先失败测试 auth state、guard、HTTP CSRF、退出清理、竞态与登录页/导航。
- [ ] 复用唯一 Axios、轻量认证状态、/login 两入口、router guard、身份导航。
- [ ] 初始化 me、登录后 /weather；USER management 拒绝；过期/退出清身份并卸载业务页缓存，旧响应不恢复身份。
- [ ] 不保存密码/令牌到 localStorage；失败退出不能伪称服务端退出成功。
- [ ] npm test / build，保留原 157 项测试。

## Task 3: portable 与当前文档

- [ ] portable 行为失败测试：导入 auth-data、登录探测字典、登出探测会话、打开 /login。
- [ ] 修改必要打包/初始化脚本，不重制 ZIP，不覆盖旧运行目录。
- [ ] 同步当前需求/架构/数据库/API/上下文；19 业务 API + 4 认证 API + health，四气象表 + 一认证表。

## Task 4: 真实验证与收口

- [ ] 新隔离 MySQL 初始化，运行认证 portable JAR；完整 mvn package 含全部数据库测试。
- [ ] USER/ADMIN HTTP、浏览器三/四页、CSRF、401/403、刷新/退出、一次临时 CRUD 清理。
- [ ] 数量 16/2/6/46080 + 2；未触及开发 3306、不凭数量宣称逐字段不变。
- [ ] 独立审查、必要修正、git diff --check 与凭据/生成物范围检查。
- [ ] 任一关键 runtime 未验证仅 IMPLEMENTED / RUNTIME_VALIDATION_PENDING，不虚构完成。

## 执行记录

Start Gate：feat/auth-role，clean；本轮不创建 worktree、不暂存/提交。采用分工实现与独立复核，所有源码留当前用户分支；中间证据保留在已忽略的 .portable-build/auth-role-work/。
