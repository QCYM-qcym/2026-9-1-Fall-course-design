# Codex 快速恢复入口

> 更新日期：2026-09-09

## 项目

- 名称：《山东省气象预报数据可视化系统的设计与实现》
- 规模：两人、两周课程设计
- 目标：在冻结方案内完成可演示、可检查、可复现的最小系统
- 当前阶段：BE-MANAGEMENT-CRUD-1 COMPLETED；Gate：READY FOR FE-MANAGEMENT-CRUD-1
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

详细证据查看 [CURRENT_CONTEXT.md](CURRENT_CONTEXT.md) 与 [v0.12-be-management-crud](../../../versions/v0.12-be-management-crud/README.md)。BE-MANAGEMENT-CRUD-1 COMPLETED；Gate：READY FOR FE-MANAGEMENT-CRUD-1。下一阶段须另行授权并完成只读开始 Gate；本轮不实现 FE Management。前置归档保持原文，Git 暂存、提交和推送由用户操作。
