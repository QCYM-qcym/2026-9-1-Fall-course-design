# Codex 快速恢复入口

> 更新日期：2026-09-08

## 项目

- 名称：《山东省气象预报数据可视化系统的设计与实现》
- 规模：两人、两周课程设计
- 目标：在冻结方案内完成可演示、可检查、可复现的最小系统
- 当前阶段：FE-TREND-1 COMPLETED；前置 PROJECT-INIT-1、DB-INIT-1、PRE-BE-WORKBENCH-FIX、BE-WORKBENCH-1、BE-DICTIONARY-1、FE-WORKBENCH-1、TREND-BACKEND-AUDIT 及 BE-TREND-1 已完成
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
- PRECIP：单位 mm，柱状图
- 需要覆盖有结果、无结果和城市切换场景

## 当前真实状态

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
- 当前分支 feat/fe-trend；本阶段开始时工作区干净，HEAD 与本地 origin/main 均为 4a528eee9866e752fb3a11dfe2e3184be0e435b1，git diff --check 通过；未 fetch。当前 FE-TREND 改动尚未提交，Git 写操作由用户通过 GitHub Desktop 完成
- GET /api/weather/trend 已实现冻结的四参数、七字段响应与四项统计；按编码解析 T2M/PRECIP 实际 ID，双系列独立排序、缺测不补零、BigDecimal/HALF_UP 两位、Empty 两空数组与四 null。Schema/Seed、Workbench 业务逻辑/SQL、Dictionary 行为和前端源码未修改
- BE-TREND 实现轮非数据库测试 121 passed / 0 failures / 0 errors / 0 skipped（Trend Service 20、Controller 31、Binding 3，既有 67）；test-compile 和限定这 121 项测试的 package/repackage PASS；该阶段前端 20 项及 build PASS。独立审查无 Critical/Important，补齐双系列不同时次缺测用例后回归通过
- 用户最终提供真实 Spring Boot + MySQL 验收结果：TrendMapperIntegrationTests 4 passed、完整 mvn test 134 passed、mvn package 134 passed/repackage PASS，均 0 failures / 0 errors / 0 skipped、BUILD SUCCESS；本轮 Codex 仅核验结果与测试源码，未重跑 Maven，不索取或记录密码。集成测试每项前后四表数量断言通过：16/2/2/192
- 用户真实 Trend HTTP：济南 ECMWF 08/11/14 时各 3 个温度/降水点，19.00/20.20/21.10℃、0.00/0.40/0.20 mm；四统计 21.10/19.00/20.10/0.60；正常 200、倒置时间 400、不存在城市 404、Empty 200/两空数组/四 null 全部通过。PowerShell 中文终端显示问题不修改业务代码
- BE-TREND 阶段用户既有 API 回归：health code=200/data=ok，字典 16/2/2，Workbench ECMWF/T2M code=200、3 times/48 records。Schema/Seed unchanged；该阶段 Gate：READY FOR FE-TREND-1，历史归档 [v0.8-be-trend](../../../versions/v0.8-be-trend/README.md) 保持原文
- /analysis 已完成城市/模型/时间范围/查询、T2M 折线图、PRECIP 柱状图、四项后端统计卡与 Loading/Empty/Error/Retry/Race Protection/Responsive；复用现有 Axios、默认编码及集中演示日期，初始化查询一次，之后按钮触发，不加 Cache、不重算统计、不按下标对齐双系列，零值保留，null/undefined 显示 --
- FE-TREND 实现轮 npm test 45 passed / 0 failed / 0 skipped（原 20 + 新 25），npm run build PASS；独立代码审查无 Critical/Important/Minor。最终文档收口未重跑测试/构建；backend/database 未修改，沿用真实后端 134 tests、Package、Trend Runtime PASS
- 用户本次人工浏览器验收全部 PASS：默认济南 + ECMWF、2026-09-07 08:00～14:00；温度 19.00/20.20/21.10℃，降水 0.00/0.40/0.20 mm，四统计 21.10/19.00/20.10/0.60；济南→青岛、ECMWF→NOAA、Loading、Empty、断开/恢复后的 Error/Retry、Race 均通过。1366×768、1920×1080、Console、/weather 地图/Timeline/City Detail 回归 PASS。此为用户真实验收证据，不冒充 Codex 本轮浏览器结果
- FE-TREND-1 COMPLETED；当前 Gate：READY FOR NEXT PHASE，归档 [v0.9-fe-trend](../../../versions/v0.9-fe-trend/README.md) 已创建。下一建议先审计 Comparison Backend，不假设 /api/weather/comparison 已实现；未开始下一阶段
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

详细证据查看 [CURRENT_CONTEXT.md](CURRENT_CONTEXT.md) 与 [v0.9-fe-trend](../../../versions/v0.9-fe-trend/README.md)。当前 FE-TREND-1 COMPLETED；Gate：READY FOR NEXT PHASE。下一建议先审计 Comparison Backend，不直接假设 /api/weather/comparison 已实现；须用户另行授权，不自动开始审计或开发。前置 v0.5～v0.8 归档保持历史原文。
