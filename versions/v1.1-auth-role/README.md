# v1.1-auth-role：认证与角色阶段归档

归档日期：2026-09-11。**AUTH-ROLE-1 COMPLETED**；Gate：**READY FOR WEATHER-UI-REFINE-1**。本归档记录已完成阶段及已有证据，不代表 Git 已提交、已打 tag、完整发布 ZIP 已生成或 UI 优化已开始。

## 完成范围

已实现 USER / ADMIN 双入口，采用 Spring Security + HttpSession（Session）+ BCrypt + CSRF。角色只由 MySQL `sys_user.role` 决定，前端入口 `loginType` 不授予权限。

| 权限范围 | USER | ADMIN |
| --- | --- | --- |
| /weather、/analysis、/comparison | 允许 | 允许 |
| 三字典与三个展示查询 API | 允许读取 | 允许读取 |
| /management | 不允许 | 允许 |
| 管理记录列表及管理类 POST/PUT/DELETE | 不允许 | 允许；写入须 CSRF |

原 **19 个天气业务 API**（16 CRUD + 3 展示查询）保持，另计 **4 个 auth API**：GET `/api/auth/csrf`、POST `/api/auth/login`、GET `/api/auth/me`、POST `/api/auth/logout`。GET `/api/health` 单独作为辅助接口，不混入两组计数。

## 数据库基线

| 项目 | 已确认数量 |
| --- | ---: |
| city | 16 |
| forecast_model | 2 |
| weather_element | 6 |
| forecast_record | 46080 |
| distinct forecast_time | 240 |
| sys_user | 2 |

当前开发库已经完成月度数据同步：192 个 city/model/element 组合各 240 条，重复业务键 0，济南/青岛/烟台抽查及 12 个旧兼容样例通过。两个认证账户 demo_user / USER、demo_admin / ADMIN 均启用，数据库保存 BCrypt 哈希，长度均为 60。本次文档归档不执行数据库写入。

## 已取得的验收证据

本节记录本阶段既有结果和用户本次确认，**本收口轮未重新运行测试、构建或运行时验收**。

- frontend：**178 PASS**；frontend build PASS。
- backend：**365 PASS**；backend package/repackage PASS。
- Portable：Windows PowerShell **5.1 认证运行验证 PASS**。PowerShell 7 失败证据保留，不描述为支持环境。
- 真实开发认证：使用独立 Cookie/Session 和每组重新获取的 CSRF，匿名 `/api/auth/me`=401；USER、ADMIN 正确入口登录=200，随后 `/me` 分别返回 demo_user / USER、demo_admin / ADMIN；USER → ADMIN 入口=403，ADMIN → USER 入口=403，错误密码=401。
- 最终浏览器人工验收：**PASS / USER_MANUAL_CONFIRMED**。USER 登录、`/analysis`、logout 正常；logout 后手动再次访问 `/analysis` 自动返回 `/login`，不显示旧趋势图或旧统计内容。该结果来自用户人工验收，不冒充 Codex 本轮浏览器执行。

## 前置运行环境恢复

LOCAL-DB-MONTHLY-SYNC 和 AUTH-LOCAL-DB-FIX 已按单独授权完成开发库数据及认证表补齐。登录 500 的根因是旧 Maven Java 进程没有继承 DB_*，回退默认数据源；正确环境的隔离实例认证通过。AUTH-RUNTIME-ENV-RECOVERY 仅重启当前开发后端以继承正确环境，完成上述真实 HTTP 复验；没有通过修改代码、配置、数据库或哈希绕过问题。

启动后端须由具备 DB_HOST、DB_PORT、DB_NAME、DB_USERNAME、DB_PASSWORD 的终端执行 `mvn spring-boot:run`，并确认新 Java 进程实际继承环境；私有凭据不写入参数、文件或归档。已有数据库不得盲目重跑完整 schema.sql/data.sql。

## 归档边界

- 本轮仅更新四份收口文档，不修改业务代码、SQL、配置、portable 业务脚本或数据库。
- [v1.0-final 历史归档](../v1.0-final/README.md) 保持原文，不复制整套源码、运行日志或数据库备份。
- Git Writes=NONE；本轮不创建分支、提交或 tag，未记录虚构 commit/tag。
- READY FOR WEATHER-UI-REFINE-1 仅表示下一阶段就绪，本轮未实施 UI 优化。
- 本地 HTTP、公开演示账户及合成天气数据不代表生产部署；完整 v1.1 发布 ZIP 未重制，PowerShell 7 未获支持确认。既有全量接口约11.25MB与共享chunk>500kB限制保留。

详细证据与当前状态：[AUTH-ROLE-1](../../课程设计/docs/AUTH-ROLE-1.md)、[CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)、[RESUME](../../课程设计/docs/context/RESUME.md)。
