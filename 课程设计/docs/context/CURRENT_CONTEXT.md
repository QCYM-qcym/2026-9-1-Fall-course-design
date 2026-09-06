# 当前项目上下文

<<<<<<< HEAD
> 更新日期：2026-09-06

## 1. 当前阶段与真实状态

《山东省气象预报数据可视化系统的设计与实现》，两人、约两周。
当前阶段：System Design V2 Freeze。
已冻结：UI V2、Backend V2、Database V2 数据范围/索引、API V2；全部软件功能仍为设计，Frontend / Backend / Database 均未初始化，无已实现 API、页面或正式测试。

仓库确实存在，审计分支 main；本轮起始 HEAD b387d5dbbc5fcefce19ba4a9d6c081b238abf7f9，origin/main acfdd7f44aa976e84028721eacaa4c5e581bdaf7。当前工作区有既有未提交修改、文档恢复和未跟踪文件，不把它们全部归为本轮变更。本轮未 fetch、commit、push、tag 或创建分支；origin/main 指本地已存在的远端跟踪引用，不声称已在线刷新。

## 2. 冻结方案

- 前端：Vue 3、Vite、JavaScript、Element Plus、ECharts、Axios、Vue Router。
- 后端：Java 8、Spring Boot 2.7.18、MyBatis-Plus、MyBatis XML、Maven；MySQL 8.0.16+。
- 测试：JUnit 5、Spring Boot Test、Postman / Apifox。
- 四页：/weather（/ 重定向）、/analysis、/comparison、/management。
- 工作台地图 P0，ECharts Map + 静态 GeoJSON；16 市，默认 ECMWF + T2M + 约 7 天演示窗口，批量加载、前端内存缓存、时间轴本地过滤。
- 趋势双要素双图、明细表与四项 Service 统计；双模型对比只做视觉比较；管理一个 Tabs 页面四类完整 CRUD。
- 五 Controller、五 Service、四 Mapper，ForecastRecordMapper.xml 承担 selectWorkbenchData、selectTrendData、selectComparisonData。
- 四表 city、forecast_model、weather_element、forecast_record，3NF；唯一键 (city_id,model_id,element_id,forecast_time)，无新增实体。
- 山东 16 市：济南、青岛、淄博、枣庄、东营、烟台、潍坊、济宁、泰安、威海、日照、临沂、德州、聊城、滨州、菏泽；核心验收济南、青岛。
- ECMWF/NOAA；T2M（2 米气温，℃）/PRECIP（降水量，mm）；16×2×2×56=3584 条仅估算，未生成。
- value 有限、满足 DECIMAL(10,2)，PRECIP ≥ 0；取消 V1 人为范围。PRECIP 为非重叠 3 小时时段量，缺测不补零、空统计 null。
- API V2 共 19 个：16 CRUD + workbench/trend/comparison。HTTP 与 code 一致；时间 yyyy-MM-dd HH:mm:ss 本地业务时间；400/404/409/500 与空匹配 200 区分。
- 预报记录二级索引仅业务唯一、workbench 联合、element_id 单列三项（另有主键）；尚未实测 EXPLAIN。

## 3. 文档与历史

当前唯一依据：

- [需求冻结 V2](../../01-立项与需求/09-需求冻结确认.md)
- [总体架构 V2](../../02-系统设计/01-总体架构设计.md)
- [前端 V2](../../02-系统设计/02-功能模块设计.md)
- [API V2](../../02-系统设计/04-接口设计.md)
- [SQL 设计 V2](../../03-数据库设计/08-SQL设计说明.md)
- [中期检查](../../05-测试与验收/01-中期检查方案.md)
- [最终验收](../../05-测试与验收/02-最终验收方案.md)
- [项目导航](../../00-项目总览/项目导航.md)
- [V2 阶段归档](../../../versions/v0.2.1-system-design-v2/README.md)

versions/v0.2-api-contract/ 原文保留为 V1 历史；其中指向当前文档的链接不是不可变 V1 正文副本。00 早期文档体系计划/设计与 06 旧项目管理计划仅为历史安排参考，旧功能/初始化口径由 V2 替代；本轮未重写 06 管理正文。2026-09-04 江苏改山东的决定继续有效。

## 4. 周期和未完成事项

保持原课程日期：9/3～18 开发与验收，9/11 中检，9/18 软件验收，9/25 材料提交。中检最低为地图、16 市、一个 ECMWF/T2M workbench 接口、时间轴、济南详情、一个趋势图、四表和真实 Git 记录；完整 CRUD、Comparison 可在中检后完成。

尚未完成：本机环境核验、工程/依赖/数据库初始化、GeoJSON 来源许可及映射、数据整理、API/GUI 实现、联调、正式测试、截图、验收记录。base package 留待初始化确认，不自行指定。设计检查不能替代运行测试。

## 5. 长期边界与协作

Monorepo：根 frontend/、backend/、database/ 保存当前实现，课程设计/ 唯一文档，versions/ 阶段说明/截图/测试/Git 信息，不复制源码、不另建重复 docs/。main + feat/fix/docs/test 短分支，真实历史由 commit/tag 保存，不伪造版本信息。

不引入登录/JWT、Redis、TDengine、ERA5、NetCDF 在线处理、实时采集、复杂 GIS、风场/高空场、模型评分训练、微服务、DDD 或自动交易。Windy 仅交互思想参考，资产和代码不复制。默认不暂存、不提交、不推送，不覆盖用户无关修改。

## 6. 下一阶段

仅建议 PROJECT-INIT-1：收到新任务后核验环境、确认 base package，依 V2 初始化 frontend/、backend/、database/，建立 versions/v0.3-project-init/ 阶段记录。旧初始化计划仅作执行顺序参考，冲突处按本次 V2。当前不得自动进入该阶段。

恢复入口：[RESUME.md](RESUME.md)。[既有任务模板（历史格式参考）](../development/TASK_TEMPLATE.md) 的旧三模块、六城举例和记录接口示例不作为当前规范；编写新任务时必须按本页链接的 V2 需求/API 填写。模板正文不在本轮允许修改范围内，保持原文。仅阶段、决策、实际完成项或下一优先级变化时更新上下文。
=======
> 更新日期：2026-09-04

## 1. 项目与当前阶段

项目名称：《山东省气象预报数据可视化系统的设计与实现》。

这是两人、两周的课程设计。当前已经完成文档体系收口、核心需求冻结、数据库详细设计确认和工程初始化规划；尚未开始正式编码、实际建库、接口联调或正式测试。

当前目录不是 Git 仓库。README 中的 Monorepo 目录、分支和命令是后续协作规范，不表示对应工程目录、分支或远端仓库已经存在。

## 2. 地域决策

- 原方案：江苏省。
- 当前冻结：山东省。
- 决策日期：2026-09-04。
- 决策原因：课程设计选题区域调整，核心业务和技术方案保持不变。
- 影响范围：项目名称、城市示例、地图增强项、演示数据和验收流程。
- 不受影响：技术栈、三大业务模块、四张核心表、API 字段和两周工程量。

## 3. 已完成并可作为依据的内容

- 项目总览：说明、导航、当前状态、文档体系实施设计与计划。
- 需求文档：背景、任务书、需求分析、功能与非功能需求、系统边界、用例、数据需求和需求冻结确认。
- 系统设计：总体架构、功能模块和核心业务流程。
- 数据库设计：需求分析、ER 模型、逻辑结构、3NF、数据字典、完整性约束、索引和待执行 SQL 设计。
- 项目管理：成员分工、两周计划、里程碑、风险清单和工程初始化计划。
- Canvas：01～10 号白板，覆盖功能、用例、架构、DFD、数据库、查询流程、开发路线和验收演示。
- 根目录 README：已记录推荐 Monorepo、Git 分支、提交、接口协作、数据库变更和敏感信息规则。

关键依据：

- [需求冻结确认](../../01-立项与需求/09-需求冻结确认.md)
- [项目当前状态](../../00-项目总览/项目当前状态.md)
- [SQL 设计说明](../../03-数据库设计/08-SQL设计说明.md)
- [工程初始化计划](../../06-项目管理/05-工程初始化计划.md)
- [仓库 README](../../README.md)

## 4. 已冻结的核心方案

- 技术栈：Vue 3 + Vite + JavaScript；Spring Boot 2.7.18 + Java 8 + MyBatis-Plus + Maven；MySQL 8.0.16+。
- 前端组件：Element Plus、ECharts、Axios、Vue Router。
- 三个模块：气象数据查询、气象数据可视化、基础数据管理。
- 四张表：`city`、`forecast_model`、`weather_element`、`forecast_record`。
- 业务唯一键：`(city_id, model_id, element_id, forecast_time)`。
- 查询条件：`cityId`、`modelId`、`elementId`、`startTime`、`endTime`，其中起止时间共同构成时间范围。
- 查询结果：城市、模型、要素、有效时间、数值和单位。
- 演示范围：济南、青岛、烟台、潍坊、临沂、济宁；核心演示使用济南和青岛；模型为 ECMWF、NOAA；要素为 T2M（℃）、PRECIP（mm）。
- 展示规则：表格和 ECharts 使用同一轮查询结果；T2M 绘制折线图，PRECIP 绘制柱状图。
- T2M 输入校验范围：`-80 ≤ value ≤ 60`；PRECIP：`0 ≤ value ≤ 1000`。

## 5. 尚未完成

- Git 仓库、远端仓库和实际开发分支的建立。
- `frontend/`、`backend/`、`database/` 工程目录的创建。
- Java、Maven、Node.js、npm、MySQL 实际版本记录。
- Spring Boot 和 Vue 工程初始化及依赖安装。
- MySQL 实际建库、建表、约束检查和初始化数据导入。
- 正式 API 契约评审与业务 API 实现。
- 查询页、管理页、表格、折线图和柱状图实现。
- 前后端联调、自动化测试、验收记录和最终演示。

规划内容不得标记为已完成，待执行 SQL 不表示数据库已经存在。

## 6. 当前优先级

在新的明确任务下，下一阶段应按[工程初始化计划](../../06-项目管理/05-工程初始化计划.md)推进：先确认本机环境和接口契约，再初始化后端、数据库和前端，最后开始核心查询链路。

当前默认只维护上下文，不自动执行工程初始化、DDL、依赖下载、Git 初始化、提交或推送。

## 7. 禁止扩展

未经需求变更确认，不加入登录权限、JWT、Redis、TDengine、NetCDF 在线解析、ERA5、实时数据、微服务、复杂预测分析或生产级部署；不增加超出四张核心表的新业务对象。山东地图仅为增强项。

## 8. 关键协作约定

- Monorepo，一个仓库内按 `frontend/`、`backend/`、`database/`、`docs/` 分工。
- `main` 为稳定分支，使用 `feat/*`、`fix/*`、`docs/*`、`test/*` 短期分支，不设置长期 `develop`。
- 数据库、接口或字段变化先评估跨层影响，并同步文档和实现。
- 不无检查执行 `git add .`，不执行破坏性 Git 操作。
- 每个任务只修改允许范围，测试未运行时必须明确说明。

## 9. 上下文维护规则

仅在项目阶段、冻结决策、实际完成项或下一优先级发生变化时更新本文件。任务操作模板见 [TASK_TEMPLATE.md](../development/TASK_TEMPLATE.md)，快速恢复入口见 [RESUME.md](RESUME.md)。
>>>>>>> acfdd7f44aa976e84028721eacaa4c5e581bdaf7
