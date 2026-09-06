# 当前项目上下文

> 更新日期：2026-09-06

## 1. 当前阶段与真实状态

《山东省气象预报数据可视化系统的设计与实现》，两人、约两周。
当前阶段：System Design V2 Freeze。
已冻结：UI V2、Backend V2、Database V2 数据范围/索引、API V2；全部软件功能仍为设计，Frontend / Backend / Database 均未初始化，无已实现 API、页面或正式测试。

本次 DOC-V2-INIT-PLAN 及经用户授权的冲突修复开始时，工作区干净，但 30 个文档/Canvas 中保留了已提交的合并冲突标记。本轮清理冲突、保留 V2 与必要历史记录，并将初始化计划修订为 V2；修改尚未提交。只读核对 main 的 HEAD 与本地 origin/main 均为 1740f78e0b7751ca65e8cbb803c0be6065dba88a；本轮未 fetch，不声称在线远端已刷新。Git 暂存、commit、push、pull、merge、tag 等写操作由用户通过 GitHub Desktop 完成。

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

versions/v0.2-api-contract/ 原文保留为 V1 历史；其中指向当前文档的链接不是不可变 V1 正文副本。00 早期文档体系计划/设计与 06 中其他旧管理计划仅为历史安排参考；[工程初始化计划 V2](../../06-项目管理/05-工程初始化计划.md) 已修订为当前初始化依据，尚未执行。2026-09-04 江苏改山东的决定继续有效。

## 4. 周期和未完成事项

保持原课程日期：9/3～18 开发与验收，9/11 中检，9/18 软件验收，9/25 材料提交。中检最低为地图、16 市、一个 ECMWF/T2M workbench 接口、时间轴、济南详情、一个趋势图、四表和真实 Git 记录；完整 CRUD、Comparison 可在中检后完成。

尚未完成：本机环境核验、工程/依赖/数据库初始化、GeoJSON 来源许可及映射、数据整理、API/GUI 实现、联调、正式测试、截图、验收记录。base package 留待初始化确认，不自行指定。设计检查不能替代运行测试。

## 5. 长期边界与协作

Monorepo：根 frontend/、backend/、database/ 保存当前实现，课程设计/ 唯一文档，versions/ 阶段说明/截图/测试/Git 信息，不复制源码、不另建重复 docs/。main + feat/fix/docs/test 短分支，真实历史由 commit/tag 保存，不伪造版本信息。

不引入登录/JWT、Redis、TDengine、ERA5、NetCDF 在线处理、实时采集、复杂 GIS、风场/高空场、模型评分训练、微服务、DDD 或自动交易。Windy 仅交互思想参考，资产和代码不复制。默认不暂存、不提交、不推送，不覆盖用户无关修改。

## 6. 下一阶段

用户先通过 GitHub Desktop 核对 main 同步状态、审查文档修复、提交并推送，使 Git 基线干净后再授权 PROJECT-INIT-1。届时核验环境、确认 base package，依工程初始化计划 V2 建立 frontend/、backend/、database/ 骨架与 versions/v0.3-project-init/ 阶段记录；不在初始化阶段实现业务 API、地图、时间轴或正式数据。当前不得自动进入该阶段。

恢复入口：[RESUME.md](RESUME.md)。[既有任务模板（历史格式参考）](../development/TASK_TEMPLATE.md) 的旧三模块、六城举例和记录接口示例不作为当前规范；编写新任务时必须按本页链接的 V2 需求/API 填写。模板正文不在本轮允许修改范围内，保持原文。仅阶段、决策、实际完成项或下一优先级变化时更新上下文。
