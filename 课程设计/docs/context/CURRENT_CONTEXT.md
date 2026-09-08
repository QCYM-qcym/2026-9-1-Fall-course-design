# 当前项目上下文

> 更新日期：2026-09-08

## 1. 当前阶段与真实状态

《山东省气象预报数据可视化系统的设计与实现》，两人、约两周。
当前阶段：BE-TREND-1 COMPLETED；前置 PROJECT-INIT-1、DB-INIT-1、PRE-BE-WORKBENCH-FIX、BE-WORKBENCH-1、BE-DICTIONARY-1、FE-WORKBENCH-1 及 TREND-BACKEND-AUDIT 已完成。当前 Gate：READY FOR FE-TREND-1，不自动开始下一阶段。
已冻结：UI V2、Backend V2、Database V2 数据范围/索引、API V2；前后端骨架与数据库初始化已完成；GET /api/weather/workbench、三个只读字典 GET 及 GET /api/weather/trend 均已完成真实验收，用户本阶段完整后端测试 134 项和打包通过。/weather 已实现并按用户人工浏览器验收确认完成收口；各项证据及未单独验证项见下文，其他业务页面仍为骨架。

当前开发机环境：Windows 11 amd64；Java 17.0.11；javac 17.0.11；Maven 3.9.16；MySQL 8.0.46（此前实测）；当前分支 feat/be-trend。个人安装路径和数据库凭据不作为项目规范。

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

已完成：本机环境与工程骨架、PROJECT-INIT-1 构建和健康检查、DB-INIT-1 四表及 16/2/2/192 数据运行验证；Workbench Controller → Service → Mapper → XML 四表 JOIN 实现及真实验收。三个字典 GET 已完成真实验收，前置完整测试 76 项、打包与 Workbench HTTP 回归通过。真实 GeoJSON 与 16 市映射、/weather 页面及该阶段真实前后端联调已按用户确认收口；Trend 后端已完成真实运行验收，完整后端 134 项及打包通过；其余 14 个业务 API、扩展数据整理、其他 GUI、课程正式验收和展示材料尚未完成。

## 5. 长期边界与协作

Monorepo：根 frontend/、backend/、database/ 保存当前实现，课程设计/ 唯一文档，versions/ 阶段说明/截图/测试/Git 信息，不复制源码、不另建重复 docs/。main + feat/fix/docs/test 短分支，真实历史由 commit/tag 保存，不伪造版本信息。

不引入登录/JWT、Redis、TDengine、ERA5、NetCDF 在线处理、实时采集、复杂 GIS、风场/高空场、模型评分训练、微服务、DDD 或自动交易。Windy 仅交互思想参考，资产和代码不复制。默认不暂存、不提交、不推送，不覆盖用户无关修改。

## 6. 下一阶段

前置阶段 PRE-BE-WORKBENCH-FIX 最终 Gate：READY FOR BE-WORKBENCH-1；IMP-01 RESOLVED，IMP-02 RESOLVED。2026-09-07 只读复核确认根目录 .DS_Store 已不再被 Git 跟踪。以下连接与打包通过记录属于该前置阶段，不代替本轮新增业务的运行验收。

- Application DB Connection：READY
- Database：shandong_weather
- Development Account：root via local environment variables
- DatabaseConnectionTests：PASS
- Backend tests：2 passed / 0 failures / 0 errors / 0 skipped
- Backend package：PASS（含 Spring Boot repackage）

验证依据：用户在包含真实本机数据库环境变量的 PowerShell 中执行 `mvn -Dtest=DatabaseConnectionTests test`、`mvn test`、`mvn package`，提供的真实输出均为 BUILD SUCCESS；单独连接测试 1 项通过，完整测试和打包中的测试均为 2 项通过。Codex 本轮未重新执行 Maven。连接测试通过真实 Spring DataSource/HikariCP/JDBC 验证 SELECT 1 = 1、SELECT DATABASE() = shandong_weather，以及 city / forecast_model / weather_element / forecast_record 数量为 16 / 2 / 2 / 192。

本课程设计本地开发与课程验收阶段使用本机 MySQL root 账号，用户名和密码仅通过环境变量注入，不写入仓库。本轮 ForecastRecordMapper 已注册，Spring Context/XML 绑定检查中未再出现 No MyBatis mapper 提示；真实 MySQL 集成测试已验证 SQL 执行成功。

前置 BE-WORKBENCH-1 COMPLETED（以下为该阶段验收记录）：

- 唯一业务 endpoint：GET /api/weather/workbench；四个必填参数、严格本地时间、400/404/空结果及稳定排序遵循 API V2；Schema 和 Seed 未修改。
- 核心城市/模型/要素编码范围在 application.yml 配置，ID、名称和单位从数据库读取；records 仅返回 cityId、cityName、forecastTime、value。
- Codex 已运行：WeatherQueryServiceTest 14、WeatherQueryControllerTest 29、WorkbenchMapperBindingTest 2、WeatherApplicationTests 1，合计 46 passed / 0 failures / 0 errors / 0 skipped；mvn test-compile PASS；前端 build PASS，原大 chunk 提示仍为非阻塞 Minor。
- 用户于 2026-09-07 在本机数据库环境变量齐备的 PowerShell 中执行并提供真实日志：Mapper 集成测试 5/0/0/0（16:03:48 BUILD SUCCESS）；完整 mvn test 52/0/0/0（16:03:53 BUILD SUCCESS）；mvn package 52/0/0/0，Spring Boot repackage 成功（16:04:02 BUILD SUCCESS）。此处依次为 tests/failures/errors/skipped，非 Codex 本轮重跑结果。
- WorkbenchMapperIntegrationTests 的 5 项真实只读测试通过：48 records / 3 times、济南合成值 19.00/20.20/21.10、NOAA、PRECIP 非负、闭区间、空结果、城市范围；每项测试后的四表数量断言仍为 16 / 2 / 2 / 192。
- Codex 已对用户启动的应用执行真实只读 HTTP 验收：ECMWF/T2M、NOAA/T2M、ECMWF/PRECIP 均为 HTTP/code 200、48 records、3 times；ECMWF/T2M 每时次 16 市；时间倒置与缺 modelId 均为 HTTP/code 400；不存在模型为 404；无数据范围为 200 且保留元数据、空数组。
- 该阶段 Gate：READY FOR NEXT PHASE。阶段归档：[v0.5-be-workbench](../../../versions/v0.5-be-workbench/README.md)。Git 提交和推送由用户在 GitHub Desktop 中检查后操作。

前置 BE-DICTIONARY-1 COMPLETED：

- 开始时只读 Git Gate 通过：feat/be-dictionary，工作区干净，HEAD 与本地 origin/main 均为 35aa067d13a0e4e9d5dad073c406f6d94a216a80；未 fetch，不声明在线远端状态。
- 新增 GET /api/cities、GET /api/forecast-models、GET /api/weather-elements：Controller → Service → MyBatis-Plus BaseMapper；三个最小 Entity 的字段与 API V2 一致，坐标为 BigDecimal，description 保留 string/null。全部字典按 id ASC 查询，不按核心编码过滤，无参数、分页或缓存；空表返回 HTTP/code 200、data=[]。
- TDD：实现前三个路由测试均因实际 404、期望 200 而失败；实现后 CityServiceTest 3、ForecastModelServiceTest 3、WeatherElementServiceTest 3、DictionaryControllerTest 9、DictionaryMapperBindingTest 3 通过。连同既有非数据库测试 46 项，合计 67 passed / 0 failures / 0 errors / 0 skipped。
- mvn test-compile PASS，包含新 DictionaryMapperIntegrationTests 的 3 项真实只读测试源码；该测试通过真实 Service/Mapper 验证字典数量、排序、编码唯一、名称、坐标、℃/mm，并在每项前后断言四表 16/2/2/192。原有测试源码未修改。
- 用户于 2026-09-07 提供本机环境变量齐备的真实日志：DictionaryMapperIntegrationTests 3/0/0/0（20:50:41 BUILD SUCCESS）；完整 mvn test 76/0/0/0（20:50:47 BUILD SUCCESS）；mvn package 76/0/0/0，Spring Boot repackage 成功（20:50:52 BUILD SUCCESS）。依次为 tests/failures/errors/skipped；本轮 Codex 未重跑数据库测试，不再保留 Runtime 待验收状态。
- DictionaryMapperIntegrationTests、DatabaseConnectionTests、WorkbenchMapperIntegrationTests 全部通过，真实 DataSource/MyBatis/MySQL 链路及四表数量 16/2/2/192 断言通过；凭据仍由本机环境变量注入，未记录密码。
- 用户启动本轮应用后，Codex 已实际只读 HTTP 验收：三个字典均 HTTP/code 200，分别返回 16/2/2；城市含经纬度，模型含 description，要素含 T2M/℃、PRECIP/mm。Workbench ECMWF/T2M 在 2026-09-07 08:00～14:00 返回 HTTP/code 200、3 times、48 records，每时次 16 个不同城市。最终收口再次请求时 8080 无法连接；本阶段采用此前同一实现的成功 HTTP 证据，不声称服务当前仍在运行。
- 前端 npm run build 在最终收口再次 PASS；既有 1,303.21 kB chunk 提示为 KNOWN NON-BLOCKING MINOR。Schema、Seed、Workbench、pom.xml、前端源码和历史归档均未修改，NO_SCHEMA_CHANGE_REQUIRED。
- 当前 Gate：READY FOR FE-WORKBENCH-1。阶段归档：[v0.6-be-dictionary](../../../versions/v0.6-be-dictionary/README.md)。下一推荐任务 FE-WORKBENCH-1，不自动开始。Git 写操作仍全部由用户通过 GitHub Desktop 完成。

前置 FE-WORKBENCH-1 COMPLETED（2026-09-08，以下为该阶段记录）：

- 开始 Git Gate：feat/fe-workbench，工作区干净；HEAD d94254655000f0b9c70f0a413e7eb9a3252c7a56，本地 origin/main 35aa067d13a0e4e9d5dad073c406f6d94a216a80；未 fetch，未执行任何 Git 写操作。
- /weather 复用现有 App Shell、Axios /api 和 Vite proxy，实现浮动日期范围、模型/要素选择、ECharts 地图、动态图例、时间轴及可关闭城市详情；仅本页面局部布局调整，未改 backend/database 或其他业务页面。
- GeoJSON：[资源与许可说明](../../../frontend/src/assets/maps/README.md)。Supeset/China-GeoData MIT 资源中筛选山东 16 个真实 Polygon/MultiPolygon，保留原始坐标；49 个闭合环、7153 个点通过几何检查；真实字典与地图匹配 16/16，unmatched=0。此前 Codex 实际点击济南、青岛、烟台 Polygon 映射正确，用户本轮确认 16 市点击正常。
- 默认按字典 code 选择 ECMWF/T2M，ID 不写死；集中演示范围为 2026-09-07 08:00:00～14:00:00，界面标记合成课程演示数据。按本轮已确认要求，色标随当前时次动态计算（本轮优先于旧设计的全响应固定色标）；简单 Map 缓存不加 TTL/LRU。
- 字典并行加载；城市 id 关联记录和坐标；缺测不补零。四参数构成缓存 key；时间轴只本地过滤；requestVersion 在缓存命中时也递增，旧成功/失败响应不能覆盖最新选择；卸载清缓存、断开 ResizeObserver 并 dispose ECharts。
- 最终收口 Codex 重新执行 npm test：20 passed / 0 failed / 0 skipped，包括真实 ECharts SVG 渲染与 dispose、16 市映射、缺测、当前时次色标、默认编码、缓存回访、旧请求晚到、空结果、错误重试、卸载失效。fixture 仅用于测试，生产页面没有 Mock API。未新增依赖，仅新增 npm test 脚本。
- 最终收口 npm run build 再次 PASS：2116 modules；主 JS 1339.62 kB（gzip 445.77 kB），Workbench 路由 377.77 kB（gzip 131.60 kB）。大 chunk 继续记为 KNOWN NON-BLOCKING MINOR，不扩大优化范围。
- 真实 API 采用此前已确认结果：三个字典 HTTP/code 200、16/2/2；Workbench HTTP/code 200、3 times/48 records。默认 ECMWF + T2M；济南 08/11/14 时分别为 19.00/20.20/21.10℃，此前 Codex 浏览器核验与用户本轮确认一致。本轮仅收口，不声明服务持续在线。
- 用户本轮人工浏览器验收确认：Timeline local filtering PASS，0 additional Workbench request；Cache PASS；ECMWF/NOAA、T2M/PRECIP、℃/mm 切换 PASS；16 市点击正常，Network 未见异常请求，整体运行无明显问题。Network 结论来源为用户人工验收，不冒充 Codex 工具直接采集结果。
- 浏览器既有证据：1366×768 有数据及城市详情打开布局 PASS，无横向溢出；Error State PASS；Console 检查无错误。Loading 状态流转、Empty、Retry、旧请求晚到保护有自动测试 PASS，但 Loading 可见过程、真实空范围 UI、后端恢复后的 Retry、快速切换乱序的专门浏览器证据未单独补齐；1920×1080 仅有 DOM 无溢出证据，完整视觉仍未单独验证。这些证据限制按用户最新收口要求如实保留，不写为全部 Runtime 子项 PASS。
- Backend regression：76-test baseline PASS，采用用户此前真实 76 passed / 0 failures / 0 errors / 0 skipped；本阶段 backend/database 未修改，本轮未重跑 Maven，不将既有基线写成本轮执行结果，不索取或记录密码。
- 只读审查未发现 Critical/Important；未发现本轮明文凭据或新增跟踪 .env/.mylogin.cnf，构建产物没有进入待提交。额外检查发现既有 `课程设计/.DS_Store` 仍被跟踪，本轮未处理（根目录 .DS_Store 的前置结论不代表递归全库无此文件）。
- 最终只读复核：待提交内容均属 FE-WORKBENCH-1；未发现 node_modules、dist、.vite、.env、.mylogin.cnf、日志或临时文件进入待提交。既有 `课程设计/.DS_Store` 记为 TRACKED_GENERATED_FILE，属于独立仓库清理项，按用户要求不阻塞完成、不执行 git rm。
- Gate：READY FOR TREND BACKEND AUDIT，依据用户最新人工验收确认及收口范围完成，而非宣称此前全部细分 Runtime Gate 已由工具验证。阶段归档：[v0.7-fe-workbench](../../../versions/v0.7-fe-workbench/README.md)。下一步建议先审计 Trend Backend，须另行授权；未开始 Trend、Comparison 或 Management。Git 暂存、提交、tag、推送仍由用户操作。

当前 BE-TREND-1 COMPLETED（2026-09-08）：

- 沿用已完成的 TREND-BACKEND-AUDIT：NO_SCHEMA_CHANGE_REQUIRED、SEED_READY_FOR_TREND；未重复审计。开始 Gate 通过：feat/be-trend、工作区干净，HEAD 与本地 origin/main 均为 274773c9d1e3b491db8b8f0c907d66ccdbfe1c62，git diff --check 通过；未 fetch，未执行任何 Git 写操作。
- 仅新增 GET /api/weather/trend：四个必填参数 cityId/modelId/startTime/endTime；严格本地时间及闭区间；响应 data 为 cityId/cityName/modelId/modelName/temperature/precipitation/statistics，统计为 temperatureMax/temperatureMin/temperatureAvg/precipitationTotal。Controller → Service → ForecastRecordMapper → XML → MySQL → TrendVO 已实现并通过用户真实运行验收。
- 复用三个字典 Mapper 检查城市/模型并按 T2M/PRECIP 编码解析实际要素 ID，不硬编码种子 ID。非法参数或非核心范围为 400、关联不存在为 404、核心要素缺失或单位异常为 500；底层异常不向客户端泄漏详情。
- selectTrendData 参数绑定查询 forecast_record JOIN weather_element，限定城市、模型、要素及时间范围，并稳定排序。两个系列独立保留真实点，缺测不补 0、不补 null；BigDecimal 统计，平均值 HALF_UP 两位。Empty 返回 HTTP/code 200、两个空数组、四项统计 null；真实零降水保持 0。
- TDD 已记录实现前路由/绑定/Service 失败及实现后通过。最终 WeatherTrendServiceTest 20、WeatherTrendControllerTest 31、TrendMapperBindingTest 3，共 54 项通过；连同既有 67 项非数据库测试，最终 121 passed / 0 failures / 0 errors / 0 skipped。独立审查无 Critical/Important；非阻塞的双系列不同步缺测漏测已补充并通过。
- mvn test-compile 已由 Codex 执行通过。用户于本次最终收口提供真实 PowerShell 结果：TrendMapperIntegrationTests 4 passed，完整 mvn test 134 passed，mvn package 134 passed 并 Spring Boot repackage PASS；三次均为 0 failures / 0 errors / 0 skipped、BUILD SUCCESS。包含既有 76 项及新增 58 项测试，非 Codex 本轮重跑结果。
- 只读核对集成测试源码：4 项覆盖济南 ECMWF、NOAA、闭区间/真实零、Empty；每项前后均断言 city/forecast_model/weather_element/forecast_record 为 16/2/2/192，并验证数据库为 shandong_weather。用户真实 4 项通过作为本阶段数据库完整性证据；Schema、Seed unchanged。
- 实现轮 Codex 限定非数据库测试的 package PASS（121 项及 repackage，10:39:22 BUILD SUCCESS）；最终完整 package 采用上述用户真实 134 项结果。Java 17、UTF-8 基线未改变。实现轮前端 npm test 20 passed、npm run build PASS；本轮文档收口未重跑构建，前端源码未修改，大 chunk 仍为既有非阻塞 Minor。
- 用户真实 HTTP 验收：济南 + ECMWF、2026-09-07 08:00:00～14:00:00，HTTP/code 200，cityId=1、modelId=1、modelName=ECMWF，temperature/precipitation 各 3 点；温度 19.00/20.20/21.10，降水 0.00/0.40/0.20，四统计依次 21.10/19.00/20.10/0.60。PowerShell 中文显示乱码按用户确认属于终端编码，既有浏览器与数据映射正确，不据此修改业务代码。
- 用户真实异常/空结果验收：startTime > endTime 为 HTTP 400；cityId=999999 为 HTTP 404；2026-09-08 08:00:00～14:00:00 为 HTTP/code 200、两个空数组、四项统计 null。上述证据来源为用户本机真实验收，不冒充 Codex 本轮直接请求结果，不声明服务持续在线。
- 用户同时确认既有 API 回归：health code=200/data=ok，三个字典数量 16/2/2，Workbench ECMWF/T2M code=200、3 times/48 records。凭据仍仅由本机环境变量提供，不索取或记录密码。
- Schema、Seed、Workbench 业务逻辑/SQL、Dictionary 行为、前端、历史归档未修改；实现轮仅既有 Workbench 两个测试适配新增构造依赖。最终收口只同步两份上下文并创建 [v0.8-be-trend](../../../versions/v0.8-be-trend/README.md) 阶段说明，不复制源码。既有课程设计/.DS_Store 仍为独立非阻塞清理项，未移除。
- Gate：READY FOR FE-TREND-1。BE-TREND-1 COMPLETED，下一建议 FE-TREND-1；未开始 FE-TREND 或 Comparison。Git 暂存、提交、tag、推送仍由用户通过 GitHub Desktop 操作，本轮 Git writes：NONE。

恢复入口：[RESUME.md](RESUME.md)。[既有任务模板（历史格式参考）](../development/TASK_TEMPLATE.md) 的旧三模块、六城举例和记录接口示例不作为当前规范；编写新任务时必须按本页链接的 V2 需求/API 填写。模板正文不在本轮允许修改范围内，保持原文。仅阶段、决策、实际完成项或下一优先级变化时更新上下文。
