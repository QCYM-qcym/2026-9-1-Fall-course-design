# Codex 快速恢复入口

## 当前续作

2026-09-11：**WEATHER-UI-REFINE-1 COMPLETED**；Gate：**READY FOR FINAL-ACCEPTANCE**。当前分支 `feat/weather-ui-refine`。先读 [CURRENT_CONTEXT](CURRENT_CONTEXT.md) 和 [v1.2-weather-ui-refine 归档](../../../versions/v1.2-weather-ui-refine/README.md)。本轮仅文档收口，未开始 FINAL-ACCEPTANCE。

- `/weather`：日期 + 当天实际时次、六要素、角度 + 中文八方位风向、地图/图例/城市详情联动和布局均已验收；默认月度范围，旧兼容样例快捷入口保留。
- `/analysis`：30 天长序列、slider + inside dataZoom、完整时间与单位 tooltip 正常，原两要素四统计含义不变。
- `/comparison`：ECMWF / NOAA 六要素对比、dataZoom、双模型 tooltip 正常，null 与 0 区分正确。
- 最终前端证据：**frontend = 193 PASS，0 failures；frontend build = PASS；git diff --check = PASS**。属于前阶段已取得证据，本收口轮未重新运行测试或构建。
- 用户本次确认的人工浏览器验收：**USER PASS、ADMIN PASS、1920×1080 PASS、1366×768 PASS**，无阻塞性布局问题；已补齐此前浏览器工具不可用所留下的待验收项。
- 认证保护：USER 仅三业务导航、无数据管理；ADMIN 保留数据管理入口；logout 后再次访问受保护页面返回 `/login`。Session + BCrypt + CSRF 与路由权限规则未改，19 个天气业务 API 与 4 个 auth API 继续分开计数。
- **Backend Modified = NO；Database Modified = NO**；沿用已确认的 16 / 2 / 6 / 46080 / 240 / 2 users 数据基线，本轮不查库、不改库。backend 365 PASS 为 AUTH 阶段历史证据，本轮未重跑。
- Known Issue：**>500 kB chunk warning**，不影响当前运行与课程设计验收，本阶段不处理。

本轮仅更新两份上下文并新增 v1.2 README；原有前端改动保留，业务代码、SQL、authentication、portable、package 配置不改。v1.0-final / v1.1-auth-role 历史归档不覆盖；Tests Re-run=NO，Git Writes=NONE。等待用户单独下达下一阶段任务，不自动执行 FINAL-ACCEPTANCE。

## AUTH-ROLE-1 历史恢复材料

以下旧 Gate、测试数字和“本轮”描述保留认证阶段当时事实；当前状态以上方为准。

2026-09-11：**AUTH-ROLE-1 COMPLETED**；Gate：**READY FOR WEATHER-UI-REFINE-1**。本轮只做文档收口，不自动开始下一阶段。先读 [CURRENT_CONTEXT](CURRENT_CONTEXT.md)、[认证说明](../AUTH-ROLE-1.md)和 [v1.1-auth-role 归档](../../../versions/v1.1-auth-role/README.md)。

- 已实现 Spring Security + HttpSession（Session）+ BCrypt + CSRF，以及 USER / ADMIN 双入口；角色只来自数据库 `sys_user.role`。
- USER 可进入 `/weather`、`/analysis`、`/comparison`，不可进入 `/management` 或调用管理记录列表、管理类写 API；ADMIN 可访问全部业务页面及管理 CRUD。四个 auth API（csrf/login/me/logout）与原 19 个天气业务 API 分开计数，health 另计。
- 当前开发库：city=16、forecast_model=2、weather_element=6、forecast_record=46080、distinct forecast_time=240、sys_user=2。192 个 city/model/element 组合各 240 条、重复业务键 0、三城市抽查及 12 个旧兼容样例通过；月度数据和认证种子已在前置任务同步完成，不再是旧 192 条开发库。
- 已取得的测试基线：frontend **178 PASS**、backend **365 PASS**；frontend build PASS、backend package/repackage PASS。本轮按用户确认记录既有证据，未重新运行测试或构建。
- Portable：Windows PowerShell **5.1 认证 Runtime PASS**；PowerShell 7 失败证据保留，不列为支持环境。未重制完整发布 ZIP，不自动升级旧 portable 数据目录。
- 前置运行时恢复后：匿名 me=401；USER、ADMIN 正确入口登录=200，me 分别返回 demo_user / USER、demo_admin / ADMIN；双向角色入口不匹配=403，错误密码=401。独立 Cookie/Session + 新 CSRF 链路通过。
- 最终人工浏览器验收 **PASS / USER_MANUAL_CONFIRMED**：USER 登录、analysis、logout 正常；logout 后手动再次进入 `/analysis` 自动返回 `/login`，旧趋势图和旧统计均不显示。本轮只记录用户提供的人工证据，不冒充新浏览器测试。

恢复提示：从已经具备 DB_HOST、DB_PORT、DB_NAME、DB_USERNAME、DB_PASSWORD 的终端，在 backend 执行 `mvn spring-boot:run`；新 Java 进程必须实际继承这五项变量，私有凭据不写入参数、文件或仓库。此前旧 8080 进程缺少全部 DB_*，回退默认配置而登录 500，已在 AUTH-RUNTIME-ENV-RECOVERY 中恢复；不应为该环境问题修改认证代码或重导 SQL。已有服务状态需按后续任务只读确认，不擅自停止或重复启动。

本轮只更新四份收口文档；业务代码、数据库和 `versions/v1.0-final` 不变，Tests Re-run=NO，Git Writes=NONE。下面为前置阶段历史恢复材料，其中旧 Gate、无认证限制、数量与测试数字仅适用于对应历史阶段；当前状态以上方和 CURRENT_CONTEXT 最新章节为准。

## 月度数据及 v1.0 历史恢复材料

2026-09-11：**MONTHLY-SYNTHETIC-DATA-1 COMPLETED**；Gate **READY FOR AUTH-ROLE-1**，只是建议，不自动实施。先读[月级数据说明](../MONTHLY-SYNTHETIC-DATA-1.md)与[实施计划](../MONTHLY-SYNTHETIC-DATA-1-plan.md)。新SQL真实16/2/6/46,080、240时次、每组240、重复0；固定seed20260901，保留济南十二值。generator10、frontend157、backend324测试全过，前端build与后端完整package通过，四页真实兼容已验收。

四表和19API不变，Trend仍两要素，Workbench/Comparison六要素。仅因真实大表不可用，预报记录增加100条本地分页，GET仍全量约11.25MB；图层面板增加滚动/换行。开发库3306未修改；新隔离实例已停止，数据与失败现场均保留。运行凭据不入Git，旧ZIP不自动升级，v1.1完整包尚未重制。浏览器清理阶段连接中断，旧压力标签可能需手动关闭；详见验收记录。下面v1.0旧状态/数量仅是历史恢复材料，Git writes = NONE。

> 更新日期：2026-09-09

## 项目

- 名称：《山东省气象预报数据可视化系统的设计与实现》
- 规模：两人、两周课程设计
- 目标：在冻结方案内完成可演示、可检查、可复现的最小系统
- 当前阶段：SYSTEM-FINALIZE-1 COMPLETED；课程设计软件 v1.0 已归档，可交用户提交，不自动进入新阶段
- 当前 Backend 基线：Java 17.0.11 + Spring Boot 2.7.18 + Maven 3.9.16

## 先读什么

1. [课程设计 AGENTS.md](../../AGENTS.md)
2. [CURRENT_CONTEXT.md](CURRENT_CONTEXT.md)
3. 用户本轮任务
4. 与任务直接相关的设计文档

若本页与 `CURRENT_CONTEXT.md` 不一致，以后者和可验证的实际文件为准。

## 技术栈

- 前端：Vue 3、Vite、JavaScript
- UI：Element Plus
- 图表：ECharts
- 请求与路由：Axios、Vue Router
- 后端：Java 17、Spring Boot 2.7.18、Maven 3.9.16
- 数据访问：MyBatis-Plus + MyBatis XML
- 构建：Maven
- 数据库：MySQL 8.0.16+；当前验证实例为 MySQL 8.0.46

## 核心范围

- Weather Workbench（地图 P0）
- Trend Analysis（双要素、四统计）
- Model Comparison（双模型视觉对比）
- Data Management（四 Tab 完整 CRUD）

## 核心数据

- `city`
- `forecast_model`
- `weather_element`
- `forecast_record`
- 业务唯一键：`(city_id, model_id, element_id, forecast_time)`

## 核心查询

- 城市：`cityId`
- 预报模型：`modelId`
- 气象要素：`elementId`
- 开始时间：`startTime`
- 结束时间：`endTime`
- API V2：16 CRUD + workbench/trend/comparison，共 19 个；按接口规定传参
- 工作台批量加载、时间轴本地切换；CRUD 后清缓存，表格/图表消费同次响应

## 演示与可视化

- 演示范围：山东全部 16 地级市，名单见 CURRENT_CONTEXT；约 3584 条为规划
- 核心演示：济南主查询、青岛切换查询
- 模型：ECMWF、NOAA
- 要素：T2M、PRECIP
- T2M：单位 ℃，折线图
- PRECIP：单位 mm；Trend 柱状图，Comparison 按最新确认采用 ECMWF/NOAA 双折线
- 需要覆盖有结果、无结果和城市切换场景

## 当前真实状态

- FE-MANAGEMENT-CRUD-1 COMPLETED：/management 四资源列表、新增、编辑、删除已完成；独立 Draft、校验与引用冲突交互、Loading/Empty/Error/Retry、防重复提交、旧响应保护及字典失效刷新均已实现。/weather、/analysis、/comparison 保持 COMPLETED；Management Backend 沿用 v0.12 已完成基线。
- 前轮真实 CRUD、描述清空 null/--、数字 ID、本地时间、PRECIP 零值/负值、重复冲突与字典刷新通过；要素 #8 删除/编码/单位修改 HTTP/code 409，记录 #202 与要素 #8 已清理。前轮真实数量 16/2/2/192，仅为数量证据，不代表全库字段逐值校验。
- USER_MANUAL_CONFIRMED：本轮用户补齐 GET 限速 Loading、受控请求阻止后的局部错误及解除后的 Retry、记录依赖字典失败恢复、四资源 Empty、1920×1080 视觉、撤销模拟后真实列表与正常 Console。Empty 为 BROWSER_SIMULATED_EMPTY；受阻请求不写成后端 HTTP 500。1366×768 及三个核心展示页回归沿用前轮有效证据。
- 最近实际 npm test 142 passed / 0 failed / 0 skipped（原 71 + 新 71），build PASS；本轮纯文档收口未重跑，未改源码/测试/配置。共享包 >500 kB、已跟踪 .DS_Store、单位冲突沿用编码文案仍为非阻塞项。
- 当前归档：[v0.13-fe-management-crud](../../../versions/v0.13-fe-management-crud/README.md)。当前分支 feat/fe-management-crud；仅同步两份上下文与新增归档，Git writes = NONE。本阶段完成不代表全系统最终验收或生产就绪。

以下三项为 v0.12 后端阶段历史，不代替上述最新前端状态：

- 最新收口（2026-09-09）：BE-MANAGEMENT-CRUD-1 COMPLETED，Management Backend 16 / 16 API（四类各 GET/POST/PUT/DELETE）。依据用户提供的本机真实 MySQL Runtime 验收结果：Runtime PASS，Initial / Final DB 均为 16 / 2 / 2 / 192，临时数据已清理；Dictionary、Workbench、Trend、Comparison regression PASS。
- ManagementCrudIntegrationTests、Full mvn test、Full mvn package / Spring Boot repackage：用户本机确认 PASS；未提供完整 Maven 测试总数，不推算。实现轮非数据库测试 273 PASS / 0 failures / 0 errors / 0 skipped；本次文档收口不重跑 Maven/HTTP、不索取密码。
- GET /api/forecast-records 全量、id ASC、冻结扩展字段，无分页/高级筛选；写操作事务、集成测试回滚，唯一/引用冲突 409，缺失 404，校验 400，未知异常 500。Schema/Seed/Index UNCHANGED。/weather、/analysis、/comparison 保持 COMPLETED；/management Frontend 仍为 PLACEHOLDER，下一阶段 FE-MANAGEMENT-CRUD-1，未自动开始。归档：[v0.12-be-management-crud](../../../versions/v0.12-be-management-crud/README.md)。本次收口分支 feat/be-management-crud，仅修改两份上下文与新归档，保留已有后端改动，Git writes = NONE。

下列为前置阶段历史证据，不代替上述最新状态。

- FE-COMPARISON 收口（2026-09-09）：/weather、/analysis、/comparison 均已有真实后端闭环；/management 仍为占位页。用户确认 Comparison Default T2M、PRECIP、City Switch、Loading/Empty/Error/Retry/Race、1366×768、1920×1080、Console 及两个既有页面回归全部 PASS。
- Comparison 默认 JINAN/T2M，2026-09-07 08:00～14:00；两要素均双折线，时间并集 ASC、缺测 null、零值保留，no cache/no metrics。前端实现轮 71 passed（45+26）/0 failed/0 skipped，build PASS，大包 warning 非阻塞；收口未重跑。
- JDBC 仅追加 allowPublicKeyRetrieval=true，保留 useSSL=false 与原环境变量机制；无账号、密码或 Schema/Seed 修改。用户确认三个字典 GET 200、三个页面恢复。此前 Codex 修复轮 Maven test/package 因仓库网络访问被拒未进入测试，不视为 PASS。归档见 [v0.11-fe-comparison](../../../versions/v0.11-fe-comparison/README.md)。
- 本次收口分支 audit/management-backend，开始工作区 clean；本轮只修改上下文与归档。下列为前置阶段历史证据，不代替最新状态。

- 核心需求已冻结
- 数据库详细设计已确认
- Database baseline ready：四表 DDL、16/2/2/192 固定种子、约束、JOIN、索引和字符集已在 MySQL 8.0.46 实际验证
- 工程初始化计划 V2 已执行并完成验收
- frontend/、backend/、database/ 工程骨架已创建
- GET /api/weather/workbench 后端调用链已完成；用户真实 Mapper 集成测试 5 项、完整测试 52 项及 package 均通过；Codex 实测正常/400/404/空结果 HTTP 行为通过，四表数量断言保持 16/2/2/192
- 三个字典 GET /api/cities、/api/forecast-models、/api/weather-elements 已完成；用户真实 DictionaryMapperIntegrationTests 3 项、完整测试 76 项、package 均通过，失败/错误/跳过均为 0。Codex 已实测三个字典 HTTP 200、16/2/2，以及 Workbench HTTP 200、3 times/48 records；四表数量断言保持 16/2/2/192。test-compile、前端 build 通过
- HTTP 验收采用此前真实 16/2/2 字典与 Workbench 3 times/48 records 成功证据；不声明服务持续在线。需要访问接口时，在已配置环境变量的终端启动后端，不索取或记录密码
- /weather 已实现真实 16 市 GeoJSON、字典与 Workbench API 客户端、浮动控件、日期范围、地图图例、城市详情、时间轴、简单内存缓存和请求竞态保护；自动测试 20 项及 build PASS，未新增依赖。当前时次动态图例遵循用户本轮确认要求
- 用户最终人工验收确认 Timeline 本地切换与 0 additional Workbench request、Cache PASS、ECMWF/NOAA、T2M/PRECIP、℃/mm 切换正常、16 市地图点击正常、济南 08/11/14 时为 19.00/20.20/21.10℃；Network 无异常，整体运行无明显问题。默认 ECMWF + T2M，GeoJSON 16 features/16 matched
- 既有真实浏览器证据：1366×768 有数据布局 PASS、Error State PASS、Console 无错误。Loading/Empty/Retry 及竞态逻辑自动测试 PASS；这些场景的专门真实浏览器验收与 1920×1080 完整视觉未单独补齐，不写成全部 Runtime 子项 PASS。按用户最新收口要求记录证据限制并完成阶段归档：[v0.7-fe-workbench](../../../versions/v0.7-fe-workbench/README.md)
- FE-WORKBENCH-1 最终收口执行 npm test：20 passed / 0 failed / 0 skipped；npm run build PASS，大 chunk 为 KNOWN NON-BLOCKING MINOR。该阶段 backend/database 未修改，按用户授权采用此前真实 backend 76-test baseline PASS，未重跑 Maven
- 待提交内容均属本阶段，未发现构建产物、.env、.mylogin.cnf、日志或临时文件进入待提交；既有 `课程设计/.DS_Store` 为 TRACKED_GENERATED_FILE，独立清理项，不阻塞本阶段，不自行移除
- 当前分支 feat/be-comparison；本阶段开始时工作区干净，HEAD 与本地 origin/main 均为 f15832089ec3f2a8431769bbc88567443e9227fd，git diff --check 通过；未 fetch。Git 写操作由用户通过 GitHub Desktop 完成
- GET /api/weather/trend 已实现冻结的四参数、七字段响应与四项统计；按编码解析 T2M/PRECIP 实际 ID，双系列独立排序、缺测不补零、BigDecimal/HALF_UP 两位、Empty 两空数组与四 null。Schema/Seed、Workbench 业务逻辑/SQL、Dictionary 行为和前端源码未修改
- BE-TREND 实现轮非数据库测试 121 passed / 0 failures / 0 errors / 0 skipped（Trend Service 20、Controller 31、Binding 3，既有 67）；test-compile 和限定这 121 项测试的 package/repackage PASS；该阶段前端 20 项及 build PASS。独立审查无 Critical/Important，补齐双系列不同时次缺测用例后回归通过
- 用户最终提供真实 Spring Boot + MySQL 验收结果：TrendMapperIntegrationTests 4 passed、完整 mvn test 134 passed、mvn package 134 passed/repackage PASS，均 0 failures / 0 errors / 0 skipped、BUILD SUCCESS；本轮 Codex 仅核验结果与测试源码，未重跑 Maven，不索取或记录密码。集成测试每项前后四表数量断言通过：16/2/2/192
- 用户真实 Trend HTTP：济南 ECMWF 08/11/14 时各 3 个温度/降水点，19.00/20.20/21.10℃、0.00/0.40/0.20 mm；四统计 21.10/19.00/20.10/0.60；正常 200、倒置时间 400、不存在城市 404、Empty 200/两空数组/四 null 全部通过。PowerShell 中文终端显示问题不修改业务代码
- BE-TREND 阶段用户既有 API 回归：health code=200/data=ok，字典 16/2/2，Workbench ECMWF/T2M code=200、3 times/48 records。Schema/Seed unchanged；该阶段 Gate：READY FOR FE-TREND-1，历史归档 [v0.8-be-trend](../../../versions/v0.8-be-trend/README.md) 保持原文
- /analysis 已完成城市/模型/时间范围/查询、T2M 折线图、PRECIP 柱状图、四项后端统计卡与 Loading/Empty/Error/Retry/Race Protection/Responsive；复用现有 Axios、默认编码及集中演示日期，初始化查询一次，之后按钮触发，不加 Cache、不重算统计、不按下标对齐双系列，零值保留，null/undefined 显示 --
- FE-TREND 实现轮 npm test 45 passed / 0 failed / 0 skipped（原 20 + 新 25），npm run build PASS；独立代码审查无 Critical/Important/Minor。最终文档收口未重跑测试/构建；backend/database 未修改，沿用真实后端 134 tests、Package、Trend Runtime PASS
- 用户本次人工浏览器验收全部 PASS：默认济南 + ECMWF、2026-09-07 08:00～14:00；温度 19.00/20.20/21.10℃，降水 0.00/0.40/0.20 mm，四统计 21.10/19.00/20.10/0.60；济南→青岛、ECMWF→NOAA、Loading、Empty、断开/恢复后的 Error/Retry、Race 均通过。1366×768、1920×1080、Console、/weather 地图/Timeline/City Detail 回归 PASS。此为用户真实验收证据，不冒充 Codex 本轮浏览器结果
- 前置 FE-TREND-1 COMPLETED；该阶段 Gate：READY FOR NEXT PHASE，归档 [v0.9-fe-trend](../../../versions/v0.9-fe-trend/README.md) 保留历史原文；其后 Comparison Audit 及 BE-COMPARISON-1 已完成
- GET /api/weather/comparison 已完成：cityId/elementId/startTime/endTime 四参数，data 为 cityId/cityName/element/series。按 modelCode 解析真实模型 ID，固定 ECMWF→NOAA，各自时间升序；缺测不补零/占位点，真实零保留，Empty 保留两系列空 values。Schema/Seed unchanged
- 用户本次真实 Runtime：Comparison Integration 4 passed，完整 mvn test 193 passed，mvn package 193 passed/repackage PASS，均 0 failures / 0 errors / 0 skipped；Spring Boot Startup PASS。四表 16/2/2/192，集成测试只读。此为用户验收证据，不冒充 Codex 收口轮执行
- Comparison HTTP 全部 PASS：济南 2026-09-07 08/11/14，T2M ECMWF 19.00/20.20/21.10、NOAA 18.40/19.60/20.50；PRECIP ECMWF 0.00/0.40/0.20、NOAA 0.10/0.60/0.30 mm。倒置时间 400、城市不存在 404、要素不存在 404；9/8 空范围 200，保留 ECMWF/NOAA 两个空 values
- 实现轮非数据库测试 176 passed、test-compile/package PASS；前端回归 45 passed/build PASS，frontend 未修改。收口只更新上下文并创建 [v0.10-be-comparison](../../../versions/v0.10-be-comparison/README.md)，未重跑测试或 HTTP，不声明服务持续在线。当前 BE-COMPARISON-1 COMPLETED，READY FOR FE-COMPARISON-1；未开始下一阶段
- 地域已于 2026-09-04 由江苏省调整并冻结为山东省

## 不要扩展

- 不做登录、用户、权限或 JWT
- 不引入 Redis、TDengine 或微服务
- 不接入 NetCDF、ERA5 或实时气象数据
- 不增加复杂预测分析和生产部署
- 不增加超出四张核心表的新业务实体
- 地图 P0 只用 ECharts + GeoJSON，不引入复杂 GIS、风场、模型评分

## 开始工作前

- 确认允许修改的文件和禁止范围
- 检查实际目录、同名文件和当前改动
- 不把规划写成已实现
- 不自行执行 DDL、下载依赖或初始化 Git
- 冻结内容需要改变且无最新明确授权时先请求确认
- 只做当前任务所需的最小修改

## 工作完成前

- 检查事实与冻结文档一致
- 运行与改动直接相关的验证
- 未运行的测试写明“未运行”和原因
- 检查无密码、令牌和本机私有配置
- 列出实际修改文件和未完成事项
- 未经要求不创建分支、不提交、不推送

## 下一任务

详细证据查看 [CURRENT_CONTEXT.md](CURRENT_CONTEXT.md)。SYSTEM-FINALIZE-1 已修复工作台临时字典污染展示范围，补 4 项回归，npm test 146 PASS / build PASS；当前文档已按批准实现最小同步，历史 [v0.13](../../../versions/v0.13-fe-management-crud/README.md) 等不变。四页正常演示与四类 GET 16/2/2/192 本轮复核成功；未受影响的 CRUD/异常/宽屏沿用已有证据，Empty 仍标浏览器模拟。

最终后端证据已补齐：用户提供本机完整 mvn package 输出，结束于 2026-09-09T13:55:12+08:00，293 tests / 0 failures / 0 errors / 0 skipped，JAR 与 Spring Boot repackage 成功，BUILD SUCCESS（8.388 s）。来源 USER_PROVIDED_LOCAL_MAVEN_OUTPUT，本次未重跑；前轮 Codex 进程缺凭据导致的 20 errors 作为历史环境失败保留在 CURRENT_CONTEXT，不再构成最终 Gate 缺项。仅更新两份上下文并创建 [v1.0-final](../../../versions/v1.0-final/README.md)，可作为课程设计软件 v1.0 交用户检查提交；不代表 Git 已提交、教师正式验收或生产部署。Git writes = NONE，不自动开始新阶段。

## v1.0 本地启动与最终复核（历史）

前置：现有 MySQL 实例及四表数据已准备，Java 17、Maven 3.9.16 与 Node/npm 已安装。不要对现有数据库重复运行 schema.sql/data.sql；凭据仅由本机环境变量提供，不写入文件。

1. 在具有 DB_HOST、DB_PORT、DB_NAME、DB_USERNAME、DB_PASSWORD 的 PowerShell 中进入仓库 backend/，执行 `mvn package`；必须完整测试成功，无跳过数据库测试。
2. 同一环境在 backend/ 执行 `mvn spring-boot:run`，保持终端运行。已有后端占用 8080 时不要重复启动或擅自终止用户进程。
3. 另一终端进入 frontend/，执行 `npm run dev -- --host 127.0.0.1`；访问输出的本机地址，通常为 http://127.0.0.1:5173，Vite 的 /api 默认代理到 localhost:8080。依赖已存在时无需重装或升级。
4. `/weather`、`/analysis`、`/comparison` 默认范围为 2026-09-07 08:00～14:00；`/management` 提供四资源 CRUD。前端复核命令为 `npm test`、`npm run build`。

本系统为合成课程演示数据，不是实时气象服务。本地 JDBC 与开发代理不是生产配置；无权限体系，不应公开暴露写接口。共享包 >500 kB、既有被跟踪 .DS_Store、要素单位引用冲突文案为已知非阻塞项；不因此扩展功能。
