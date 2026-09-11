# AUTH-ROLE-1 认证与角色

2026-09-11 收口：**AUTH-ROLE-1 COMPLETED**；Gate：**READY FOR WEATHER-UI-REFINE-1**。用户根据中期建议批准 USER / ADMIN，当前实施与验收记录见本页、[实施计划](AUTH-ROLE-1-plan.md)及 [v1.1-auth-role 阶段归档](../../versions/v1.1-auth-role/README.md)。本轮仅整理既有证据，不开始 UI 优化。

## 方案及权限

单体同源（开发 Vite proxy，portable 合并 JAR）采用 Spring Security + HttpSession + BCrypt + CSRF，无 JWT、Redis、OAuth、复杂 RBAC。角色仅取 MySQL sys_user.role，入口 loginType 不授予权限。

| 请求/页面 | 未认证 | USER | ADMIN |
| --- | --- | --- | --- |
| /login、登录静态资源、GET health/csrf | 公开 | 公开 | 公开 |
| /weather、/analysis、/comparison | 跳登录 | 允许 | 允许 |
| GET 三字典、三个展示 API | 401 | 允许 | 允许 |
| GET /api/forecast-records、四资源 POST/PUT/DELETE | 401（无 CSRF 的写请求先 403） | 403 | 允许，写请求须 CSRF |
| /management | 跳登录 | 前端拒绝并返回 /weather | 允许 |

SPA 静态外壳可公开下载，不包含业务数据；后端 API 执行真实权限控制。菜单隐藏不作为授权依据。错误密码、用户不存在、disabled 返回 401；凭据正确但入口与角色不符返回 403，不建立认证 Session。JSON 结构/入口值不合法返回 400。

## 四个认证接口（另计）

均使用 ApiResponse；原 19 个气象业务接口不改业务结构，另有 4 个认证接口和 GET /api/health 辅助接口。

- GET /api/auth/csrf：data.token / headerName / parameterName；XSRF-TOKEN Cookie 可由客户端读取，写请求使用 X-XSRF-TOKEN。
- POST /api/auth/login：JSON username/password/loginType，成功 data.id/username/role。
- GET /api/auth/me：成功同上，匿名或 Session 失效 401。
- POST /api/auth/logout：使 Session 失效，返回成功 data:null。

登录和退出也携带 CSRF；Axios 复用现有实例，每次写入先取当前 Token，不自动重放失败写请求。JSESSIONID HttpOnly、SameSite=Lax；登录启用 Session fixation 防护，Session 与响应仅保留安全身份，不含密码/哈希。仅本地 HTTP 演示，生产还需 HTTPS、Secure Cookie、真实凭据与安全运维，本阶段不宣称生产就绪。

## 初始化与演示账户

4 张气象业务表 + 1 张认证表 sys_user。新增字段 id、username 唯一、password_hash、role、enabled，角色 CHECK 限制为 USER/ADMIN。天气 data.sql 与生成器不包含认证账户。

**DEMO ONLY / NOT FOR PRODUCTION**：下列公开账户只用于合成课程演示，不是 MySQL 连接账号或开发机密码。

| 用户名 | 公开演示密码 | 登录入口 |
| --- | --- | --- |
| demo_user | DemoUser@2026 | 普通用户 |
| demo_admin | DemoAdmin@2026 | 管理员 |

仅在新空隔离数据库导入 `database/schema.sql` → `database/data.sql` → `database/auth-data.sql`。schema 包含破坏性重建语句，禁止为给旧库增加账号而直接重跑整个 schema。初始实现阶段使用新隔离实例；后续用户已单独授权 LOCAL-DB-MONTHLY-SYNC 和 AUTH-LOCAL-DB-FIX：先备份旧开发数据并按正式月度 SQL 同步天气数据，再仅创建缺失的 sys_user、导入认证种子，核对 DDL 和 BCrypt 哈希。旧 portable var 仍不自动迁移。数据库只保存 BCrypt hash；不返回 hash，不缓存密码。本次文档收口不再执行任何数据库写入。

当前开发数据库 `shandong_weather` 已确认：city=16、forecast_model=2、weather_element=6、forecast_record=46080、distinct forecast_time=240、sys_user=2。两个演示账号 role 分别为 USER/ADMIN，enabled=1，password_hash 长度均为 60。月度数据有 192 个 city/model/element 组合，每组 240 条，重复业务键 0；济南/青岛/烟台抽查及 12 个旧兼容样例通过。天气生成器重新生成 data.sql 不触及 auth-data.sql。MONTHLY-SYNTHETIC-DATA-1 完成事实及 v1.0 历史保持。

## 前端与启动

`/login` 两入口、初始化 me 恢复 Session、两角色登录后均到 /weather。USER 无管理导航且直达管理页被拒；退出/401 会话失效卸载业务组件并清除其局部缓存，旧请求不能恢复身份。失败退出明确提示未确认服务端失效，不谎报成功。

开发启动仍为具备正确本机数据库环境的 backend `mvn spring-boot:run` + frontend `npm run dev`；不要把私有环境变量写入仓库。当前开发库的 sys_user 已补齐，不重复初始化。新目标空库仍须按上述初始化顺序准备数据。

portable 新构建复制三份 SQL，首次依次导入；以临时 USER Session 查询真实城市字典，探测后 logout，再打开 /login。后续保留数据，不重导、不自动升级旧目录；本阶段不重制完整 ZIP。

## 验证记录

本节汇总已经取得的测试、前置运行时验证及用户人工验收。AUTH-ROLE-1-CLOSEOUT 本轮未重新运行测试、构建、HTTP 或浏览器验收。

| 证据范围 | 已取得结果 | 来源与边界 |
| --- | --- | --- |
| frontend | 178 PASS；build PASS | 本阶段历史测试/构建基线，用户本次确认；不是收口轮新执行 |
| backend | 365 PASS；package/repackage PASS | 本阶段历史测试/构建基线，用户本次确认；不是收口轮新执行 |
| Portable Windows PowerShell 5.1 | 认证运行验证 PASS | 既有真实 Runtime，用户本次确认 |
| Portable PowerShell 7 | 失败证据保留 | 不列为支持环境，不将模拟断言替代 Runtime |
| 开发库 | 16 / 2 / 6 / 46080 / 240 / 2 users | 前置任务真实 MySQL 查询及月度数据严格核验 |
| 浏览器最终退出回归 | PASS / USER_MANUAL_CONFIRMED | 用户提供的最终人工验收，具体流程见下文 |

早期 frontend 177 项、portable 11 项模拟认证断言和 94 项既有安全断言属于实施中间证据，保留其历史来源；当前最终测试基线为上表的 178 / 365。早期浏览器工具连接失败不作为通过证据；最终浏览器缺项由用户人工验收补齐，不虚构工具重测。

### 真实开发认证验收

AUTH-RUNTIME-ENV-RECOVERY 已在 localhost:8080 重新验收。每组登录使用独立 Session/Cookie，先 GET `/api/auth/csrf` 再携带返回的 Token/Cookie 登录，没有关闭 CSRF 或绕过 Spring Security。

| 场景 | 实际结果 |
| --- | --- |
| anonymous GET /api/auth/me | 401 |
| demo_user + USER | POST login 200；GET me 200，demo_user / USER |
| demo_admin + ADMIN | POST login 200；GET me 200，demo_admin / ADMIN |
| demo_user + ADMIN 入口 | 403 |
| demo_admin + USER 入口 | 403 |
| 错误密码 | 401 |

USER / ADMIN 页面及 API 权限边界已由本阶段既有验收和用户本次确认收口。最终人工浏览器流程：USER 登录正常 → `/analysis` 正常 → logout 正常 → 手动再次访问 `/analysis` → 自动返回 `/login`，不显示旧趋势图、不显示旧统计内容；结果 **PASS / USER_MANUAL_CONFIRMED**。

### 登录 500 的环境根因与恢复记录

补齐认证表后，旧 8080 的 Maven 子进程仍返回登录 500。AUTH-RUNTIME-500-ROOT-CAUSE 只读确认该 Java 进程的 DB_HOST、DB_PORT、DB_NAME、DB_USERNAME、DB_PASSWORD 全部缺失，按 application.yml 回退默认数据源；当前 Codex 环境可读取数据库并不代表已启动的 Java 进程继承了凭据。相同认证类及配置在使用正确 DB_* 的隔离 JAR 实例中 USER/ADMIN 登录均 200、错误密码 401，确认运行环境差异。

AUTH-RUNTIME-ENV-RECOVERY 经用户授权仅停止旧项目后端，从 backend 以正确环境重新执行 `mvn spring-boot:run`，直接确认新 Java 进程五项 DB_* 均 AVAILABLE，health=200，并取得上表认证结果。恢复没有修改源码、配置、数据库或 BCrypt 哈希。数据库最终数量仍为 16 / 2 / 6 / 46080 / 240 / 2 users。私有连接凭据、进程环境内容和临时日志不进入版本归档。

## 收口范围与限制

本轮只更新 CURRENT_CONTEXT、RESUME、本页和 v1.1-auth-role/README；原有实现改动保留，不修改 Java、Vue、SQL、package 配置或 portable 业务脚本，不修改数据库，不重跑完整测试。`versions/v1.0-final` 不覆盖，不创建或虚构 Git commit/tag，Git Writes=NONE。READY FOR WEATHER-UI-REFINE-1 仅为 Gate，尚未启动 UI 优化。

本机 HTTP 与公开演示账户仅课程使用；无用户管理/注册/密码重置/生产登录限流，不宣称生产就绪。全量记录接口约11.25MB及共享chunk>500kB为已有非阻塞限制；未重制完整 v1.1 发布 ZIP。
