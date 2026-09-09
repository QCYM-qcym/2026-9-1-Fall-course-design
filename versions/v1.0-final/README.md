# v1.0 — 课程设计软件最终版

Status：SYSTEM-FINALIZE-1 COMPLETED

日期：2026-09-09。Gate：可作为课程设计软件 v1.0 交用户检查提交；不代表已 commit/tag/push、教师正式验收或生产就绪。

## 范围与实现

基于 [v0.13](../v0.13-fe-management-crud/README.md) 完成聚焦最终审查、必要修复与验证。四页面及 19 个业务 API 完整保留：四资源 16 个 CRUD 接口与 workbench/trend/comparison 三个展示查询；健康检查不计入业务 API。

- `/weather`：山东 16 市地图、ECMWF/NOAA、T2M/PRECIP、时间轴本地过滤、详情与动态色标；缓存限页面实例，离页清空并拒绝旧响应。
- `/analysis`：城市/模型/时间范围，T2M 折线、PRECIP 柱状、四项后端统计；无明细表、无缓存。
- `/comparison`：ECMWF/NOAA 两模型，T2M/PRECIP 均双折线；时间并集 ASC 对齐、缺测 null、真实零保留；无误差指标或评分。
- `/management`：城市、模型、要素、记录四 Tab 完整 CRUD；专用写请求、独立草稿、引用/唯一冲突、刷新/字典失效、Loading/Empty/Error/Retry 与竞态保护。记录全量 id ASC，无分页或筛选。

最终审查仅修复 [workbenchState.js](../../frontend/src/utils/workbenchState.js) 将管理临时字典混入展示的问题：先按核心编码过滤，再校验 16 市映射和选择默认项；管理字典保持全量。新增四项 [回归测试](../../frontend/src/utils/workbenchState.test.js)，三类临时字典用例实际 RED→GREEN，缺核心城市仍报错。同步当前需求/设计/验收文档中的过时说明，不恢复旧规划、不重写历史归档。

技术栈保持 Vue 3/Vite/Element Plus/ECharts/Axios；Java 17/Spring Boot 2.7.18/MyBatis-Plus/MyBatis XML/Maven 3.9.16/MySQL 8。没有新增功能、升级依赖或修改 Schema/Seed。

## 验证结果与来源

| 验证 | 实际结果 | 证据来源 |
| --- | --- | --- |
| frontend npm test | 146 passed，0 failed，0 skipped；Node 92 + Vitest 54，原有 142 + 新增 4 | SYSTEM-FINALIZE-1 前轮 Codex 实际执行，本次沿用 |
| frontend npm run build | PASS；共享包 >500 kB warning | 同上，不重复构建 |
| backend mvn package | 293 tests，0 failures，0 errors，0 skipped；JAR、repackage、BUILD SUCCESS | 用户最新本机输出，USER_PROVIDED_LOCAL_MAVEN_OUTPUT；本次核验，不冒充 Codex 重跑 |
| 四页正常演示 | PASS；工作台 16/16 有值、济南详情，趋势双图四统计，对比双模型各 3 点，管理真实列表/本地时间/零值正常 | SYSTEM-FINALIZE-1 前轮浏览器实际执行 |
| 四类 GET 数量 | HTTP/code 200；16/2/2/192 | 同阶段前轮真实只读 HTTP |
| 工作台默认数据 | code 200，3 times/48 records，济南 T2M 19.00/20.20/21.10℃ | 同阶段前轮真实 HTTP |
| 趋势默认数据 | 两序列各 3 点；T2M 19.00/20.20/21.10，PRECIP 0.00/0.40/0.20；四统计 21.10/19.00/20.10/0.60 | 同阶段前轮真实 HTTP/浏览器 |
| 对比默认 T2M | ECMWF 19.00/20.20/21.10；NOAA 18.40/19.60/20.50 | 同阶段前轮真实 HTTP/浏览器 |
| 正常 Console | 检查会话仅 Vite 连接调试日志，无错误/警告 | 同阶段前轮浏览器检查 |
| 未受修改影响的 CRUD、引用保护、错误恢复、Race、两档宽屏 | 沿用有效阶段验收；1366×768、1920×1080 | v0.13 及其引用的前轮实际操作、用户人工确认，不声称全部本次重测 |

用户 Maven 日志结束时间为 2026-09-09T13:55:12+08:00，耗时 8.388 s；jar:3.2.2 生成 `weather-backend-0.3.0-SNAPSHOT.jar`，spring-boot:2.7.18:repackage 明确替换主归档。软件归档版本 v1.0 不改变既有 Maven/npm 工程版本号。

前轮 Codex 完整 package 因执行进程缺少数据库凭据而产生 20 项连接错误（293 tests，273 passed），属于环境失败；此次用户真实完整成功输出补齐 Gate，不删除历史失败记录，不读取或保存密码。

Management 四资源 Empty 保留 **BROWSER_SIMULATED_EMPTY / USER_MANUAL_CONFIRMED** 标注，不是 REAL_MYSQL_EMPTY_PASS。受控 GET 网络阻止及恢复不等同后端 HTTP 500。修复后的临时字典边界采用自动测试，本阶段没有重复临时数据 HTTP 写入验收。

数量 16/2/2/192 仅证明数量基线，不证明所有字段逐值未变；v0.13 临时记录 #202、要素 #8 的清理沿用该阶段证据。本次文档收口不写库、不重新操作临时资源。

## 本地启动

使用现有已初始化的 MySQL 四表及本机 Java 17、Maven 3.9.16、Node/npm；不要对已有实例重跑初始化脚本。数据库凭据仅由 DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD 本地环境变量提供。

1. 在有真实数据库环境变量的终端进入 `backend/`：完整验证命令为 `mvn package`（含数据库测试），启动命令为 `mvn spring-boot:run`；保持终端运行，已有服务时不要重复占用 8080。
2. 另一终端进入 `frontend/`：`npm run dev -- --host 127.0.0.1`，访问输出地址，通常 http://127.0.0.1:5173；Vite `/api` 默认代理到 localhost:8080。已有依赖无需重装或升级。
3. 默认演示范围为 2026-09-07 08:00:00～14:00:00，三个时次 08/11/14；工作台 ECMWF/T2M、趋势济南/ECMWF、对比济南/T2M。

## 已知限制与安全边界

- 使用固定合成课程演示数据，不是实时气象服务；仅山东 16 市、两模型、两要素、192 条记录，七天/3584 条是早期规划而非现有数据。
- 本地开发配置不等于生产部署配置；既有 JDBC `useSSL=false&allowPublicKeyRetrieval=true`、本机账号、开发代理不能视作生产安全方案。无认证权限，不应公开暴露管理写接口。
- 共享包 >500 kB 警告、既有跟踪的 `课程设计/.DS_Store`、要素单位引用冲突沿用编码冲突文案，均为保留的非阻塞项。
- Workbench 缓存为实例内存，无容量淘汰/跨客户端推送；外部改库后重新进入或重载页面取新数据。无实时采集、导入导出、模型误差指标或生产级能力。

## 文档收口与 Git

本次只更新 [CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)、[RESUME](../../课程设计/docs/context/RESUME.md) 并创建本归档；保留前轮所有改动，不修改业务代码、依赖、数据库或历史版本，不复制源码、构建产物和日志。

当前分支 main。Git writes = NONE；不虚构 commit、tag 或 push。用户通过 GitHub Desktop 检查并提交课程设计软件 v1.0；不自动开始新阶段。
