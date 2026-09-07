# 当前项目上下文

> 更新日期：2026-09-07

## 1. 当前阶段与真实状态

《山东省气象预报数据可视化系统的设计与实现》，两人、约两周。
当前阶段：PROJECT-INIT-1 completed；DB-INIT-1 COMPLETED；PRE-BE-WORKBENCH-FIX COMPLETED。
已冻结：UI V2、Backend V2、Database V2 数据范围/索引、API V2；前端、后端和数据库工程骨架已初始化，数据库 SQL、种子数据及 MySQL 8.0.46 运行验证已完成，业务功能仍未开始。

当前开发机环境：Windows 11 amd64；Java 17.0.11；javac 17.0.11；Maven 3.9.16；MySQL 8.0.46；当前分支 feat/db-init。个人安装路径和数据库凭据不作为项目规范。

本次 DOC-V2-INIT-PLAN 及经用户授权的冲突修复开始时，工作区干净，但 30 个文档/Canvas 中保留了已提交的合并冲突标记。本轮清理冲突、保留 V2 与必要历史记录，并将初始化计划修订为 V2；修改尚未提交。只读核对 main 的 HEAD 与本地 origin/main 均为 1740f78e0b7751ca65e8cbb803c0be6065dba88a；本轮未 fetch，不声称在线远端已刷新。Git 暂存、commit、push、pull、merge、tag 等写操作由用户通过 GitHub Desktop 完成。

## 2. 冻结方案

- 前端：Vue 3、Vite、JavaScript、Element Plus、ECharts、Axios、Vue Router。
- 后端：Java 17、Spring Boot 2.7.18、MyBatis-Plus、MyBatis XML、Maven 3.9.16；MySQL 8.0.16+。
- 测试：JUnit 5、Spring Boot Test、Postman / Apifox。
- 四页：/weather（/ 重定向）、/analysis、/comparison、/management。
- 工作台地图 P0，ECharts Map + 静态 GeoJSON；16 市，默认 ECMWF + T2M + 约 7 天演示窗口，批量加载、前端内存缓存、时间轴本地过滤。
- 趋势双要素双图、明细表与四项 Service 统计；双模型对比只做视觉比较；管理一个 Tabs 页面四类完整 CRUD。
- 五 Controller、五 Service、四 Mapper，ForecastRecordMapper.xml 承担 selectWorkbenchData、selectTrendData、selectComparisonData。
- 四表 city、forecast_model、weather_element、forecast_record，3NF；唯一键 (city_id,model_id,element_id,forecast_time)，无新增实体。
- 山东 16 市：济南、青岛、淄博、枣庄、东营、烟台、潍坊、济宁、泰安、威海、日照、临沂、德州、聊城、滨州、菏泽；核心验收济南、青岛。
- ECMWF/NOAA；T2M（2 米气温，℃）/PRECIP（降水量，mm）；MySQL 已实际导入 16×2×2×3=192 条固定合成演示记录，规划规模 16×2×2×56=3584 条仍仅为估算。
- value 有限、满足 DECIMAL(10,2)，PRECIP ≥ 0；取消 V1 人为范围。PRECIP 为非重叠 3 小时时段量，缺测不补零、空统计 null。
- API V2 共 19 个：16 CRUD + workbench/trend/comparison。HTTP 与 code 一致；时间 yyyy-MM-dd HH:mm:ss 本地业务时间；400/404/409/500 与空匹配 200 区分。
- 预报记录二级索引仅业务唯一、workbench 联合、element_id 单列三项（另有主键）；MySQL 8.0.46 已实测 SHOW INDEX 与 EXPLAIN，小数据集实际选择 element_id 索引，不扩展冻结索引。

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

versions/v0.2-api-contract/ 原文保留为 V1 历史；其中指向当前文档的链接不是不可变 V1 正文副本。00 早期文档体系计划/设计与 06 中其他旧管理计划仅为历史安排参考；[工程初始化计划 V2](../../06-项目管理/05-工程初始化计划.md) 已修订并完成 PROJECT-INIT-1 验收。2026-09-04 江苏改山东的决定继续有效。

## 4. 周期和未完成事项

保持原课程日期：9/3～18 开发与验收，9/11 中检，9/18 软件验收，9/25 材料提交。中检最低为地图、16 市、一个 ECMWF/T2M workbench 接口、时间轴、济南详情、一个趋势图、四表和真实 Git 记录；完整 CRUD、Comparison 可在中检后完成。

已完成：本机 Java 17.0.11、Maven 3.9.16、Node.js、npm、MySQL 8.0.46 核验；frontend/、backend/、database/ 骨架初始化；前端构建、后端 Context 测试、jar 打包、major version 61 和健康检查验证；数据库四表 DDL、16/2/2/192 数据导入、约束、JOIN、索引、字符集运行验证。尚未完成：GeoJSON 来源许可及映射、扩展数据整理、19 个业务 API、GUI 业务实现、联调、正式测试、截图和验收记录。设计检查不能替代运行测试。

## 5. 长期边界与协作

Monorepo：根 frontend/、backend/、database/ 保存当前实现，课程设计/ 唯一文档，versions/ 阶段说明/截图/测试/Git 信息，不复制源码、不另建重复 docs/。main + feat/fix/docs/test 短分支，真实历史由 commit/tag 保存，不伪造版本信息。

不引入登录/JWT、Redis、TDengine、ERA5、NetCDF 在线处理、实时采集、复杂 GIS、风场/高空场、模型评分训练、微服务、DDD 或自动交易。Windy 仅交互思想参考，资产和代码不复制。默认不暂存、不提交、不推送，不覆盖用户无关修改。

## 6. 下一阶段

PRE-BE-WORKBENCH-FIX 最终 Gate：READY FOR BE-WORKBENCH-1；IMP-01 RESOLVED，IMP-02 RESOLVED。2026-09-07 只读复核确认根目录 .DS_Store 已不再被 Git 跟踪。

- Application DB Connection：READY
- Database：shandong_weather
- Development Account：root via local environment variables
- DatabaseConnectionTests：PASS
- Backend tests：2 passed / 0 failures / 0 errors / 0 skipped
- Backend package：PASS（含 Spring Boot repackage）

验证依据：用户在包含真实本机数据库环境变量的 PowerShell 中执行 `mvn -Dtest=DatabaseConnectionTests test`、`mvn test`、`mvn package`，提供的真实输出均为 BUILD SUCCESS；单独连接测试 1 项通过，完整测试和打包中的测试均为 2 项通过。Codex 本轮未重新执行 Maven。连接测试通过真实 Spring DataSource/HikariCP/JDBC 验证 SELECT 1 = 1、SELECT DATABASE() = shandong_weather，以及 city / forecast_model / weather_element / forecast_record 数量为 16 / 2 / 2 / 192。

本课程设计本地开发与课程验收阶段使用本机 MySQL root 账号，用户名和密码仅通过环境变量注入，不写入仓库。当前未创建业务 Mapper，No MyBatis mapper 提示为预期非阻塞提示，不为消除提示创建空 Mapper 或修改扫描配置。

PROJECT-INIT-1、DB-INIT-1 与 PRE-BE-WORKBENCH-FIX 均已完成；未实现业务 API、地图或时间轴。下一阶段建议 `BE-WORKBENCH-1`，不自动进入。

恢复入口：[RESUME.md](RESUME.md)。[既有任务模板（历史格式参考）](../development/TASK_TEMPLATE.md) 的旧三模块、六城举例和记录接口示例不作为当前规范；编写新任务时必须按本页链接的 V2 需求/API 填写。模板正文不在本轮允许修改范围内，保持原文。仅阶段、决策、实际完成项或下一优先级变化时更新上下文。
