# 当前项目上下文

## MONTHLY-SYNTHETIC-DATA-1 当前工作

2026-09-11：**MONTHLY-SYNTHETIC-DATA-1 COMPLETED**。完整证据见[月级数据说明](../MONTHLY-SYNTHETIC-DATA-1.md)，步骤见[实施计划](../MONTHLY-SYNTHETIC-DATA-1-plan.md)。固定seed20260901，2026-09-01～30每日8时次；实际SQL与隔离MySQL均为16/2/6/46,080、240时次、192组各240条、重复0。T2M/PRECIP/TCC由共享合成天气过程构造，u/v推导风速风向，T2M/内部露点推导RH；ECMWF/NOAA是模型场景，不是真实气象源。

最终本轮实际验证：generator10 PASS；frontend157 PASS（Node98+Vitest59）、build PASS；backend完整package324/0/0/0，含真实DB测试，JAR/repackage PASS。四表Schema、19个业务API、Trend两要素四统计不变，Workbench/Comparison六要素真实查询与页面显示通过；济南十二个旧样例保持。大表实测34.6秒并交互超时，按用户明确例外仅记录表加100条本地分页；GET仍全量ID升序。六图层面板遮挡以局部滚动/换行最小修复，未整体美化。

隔离目录`.portable-build/monthly-20260911-verified`导入新SQL并重启保持数据，最终已正常停止；不使用开发库3306，首次失败目录和既有失败现场保留。旧大表压力标签关闭时浏览器连接中断，关闭及视口复原未确认，可能需用户手动关闭。全量接口约11.25MB和chunk>500kB仍为限制；旧v1.0 ZIP不自动更新，本轮未重制910MB包。历史v1.0与中期报告未改，私有运行文件不入Git。Gate：**READY FOR AUTH-ROLE-1**，未开始下一阶段，Git writes = NONE。

以下保留 v1.0 收口及其历史证据。

> 更新日期：2026-09-09

## 1. 当前阶段与真实状态

《山东省气象预报数据可视化系统的设计与实现》，两人、约两周。
当前阶段：SYSTEM-FINALIZE-1 COMPLETED。用户补充的本机完整 mvn package 日志已核验：293 tests / 0 failures / 0 errors / 0 skipped，JAR 与 Spring Boot repackage 成功，BUILD SUCCESS；最终验证缺项已补齐。课程设计软件 v1.0 可交由用户检查并提交，归档见 [v1.0-final](../../../versions/v1.0-final/README.md)，不代表已提交、教师正式验收或生产就绪。
Management Backend 沿用 v0.12 已完成的 16 / 16 API 基线；四页面均已有真实后端闭环，19 个业务 API 保持兼容。前端沿用本阶段前轮实际验证 146 passed / 0 failed / 0 skipped、build PASS；后端采用用户本机最新日志，不冒充 Codex 重跑。Comparison 采用 T2M、PRECIP 双折线，管理记录 GET 全量 id ASC、无筛选；没有业务范围扩展。

当前开发机环境：Windows 11 amd64；Java 17.0.11；javac 17.0.11；Maven 3.9.16；MySQL 8.0.46（此前实测）；检查分支 main，审查轮开始时 clean，最终文档收口保留此前 16 个修改文件，未切换分支。个人安装路径和数据库凭据不作为项目规范。

### SYSTEM-FINALIZE-1 本轮结果（2026-09-09）

- 审查与修复：工作台原来直接使用全量字典，管理新增临时城市会破坏严格 16 市映射，临时模型/要素也会成为不受后端支持的选项。现在先按核心编码筛选再映射/选默认值，管理字典保持全量。新增三类临时字典测试先实际失败，再最小修复通过；另补缺核心城市不得被临时城市替代的测试。独立复核未发现重要遗留问题。
- frontend：本轮 npm test 146 PASS（Node 92 + Vitest 54），0 failed / 0 skipped；原有 142 项保留，新增 4 项。npm run build PASS，共享包 >500 kB 警告未优化。
- backend 历史尝试：Codex 前轮执行完整 mvn package，293 tests / 0 failures / 20 errors / 0 skipped，其中 273 passed；因该进程未取得数据库凭据，默认 example_user 无密码连接被拒绝而 BUILD FAILURE。该环境失败记录保留，不判为代码失败，不改凭据。
- backend 最终通过证据：用户在具备真实本机数据库环境的 PowerShell 执行完整 mvn package，提供日志结束时间 2026-09-09T13:55:12+08:00、耗时 8.388 s；293 tests / 0 failures / 0 errors / 0 skipped，jar:3.2.2 生成 weather-backend-0.3.0-SNAPSHOT.jar，spring-boot:2.7.18:repackage 替换主归档，BUILD SUCCESS。来源 USER_PROVIDED_LOCAL_MAVEN_OUTPUT；本次仅核验，不重跑测试，不索取或记录密码。由此解除最终验证阻塞。
- 本轮真实只读 HTTP：四类 GET 均 HTTP/code 200，数量 16/2/2/192；Workbench code 200、3 times/48 records、济南 T2M 19.00/20.20/21.10；Trend 与 Comparison 正常响应。首次受工具代理影响的 GET 502 未作为后端失败证据，直连本机复核成功。
- 本轮浏览器：修复后重新进入 /weather，16/16 有值和边界匹配、济南 19.00℃、三个时次正常；/analysis 济南/ECMWF、两图各 3 样本，四统计 21.10/19.00/20.10/0.60；/comparison 默认 T2M 双模型各 3 点；/management 城市与记录真实列表、名称、本地时间和 0.00 正常。新浏览器会话 Console 仅 Vite 连接调试日志，无错误/警告。本轮未做临时字典 HTTP 写入；修复的临时字典边界证据为自动测试，不冒充真实新增后的浏览器测试。
- 未受修改影响的 CRUD、引用保护、Race、Error/Retry、1366×768 与 1920×1080 采用 v0.13 及其引用的已有验收证据；Management Empty 仍为 BROWSER_SIMULATED_EMPTY / USER_MANUAL_CONFIRMED，不是 REAL_MYSQL_EMPTY_PASS。
- 本轮无 HTTP 写入、无新增临时 ID，数量基线不等同全库字段逐值未变。backend、database、Schema/Seed、依赖及历史 versions/ 均未修改。当前需求/架构/模块/API/流程/验收与导航纠正工程未创建、七天窗口、Comparison 柱状、Trend 明细表/跨页传参及管理筛选等过时说明，不恢复旧设计行为。
- 系统使用合成课程演示数据，不是实时气象服务；本地 JDBC useSSL=false/allowPublicKeyRetrieval=true、开发代理及本机账号方式不是生产部署方案。无认证权限，不应公开暴露管理写接口；本轮不扩展生产能力。
- 最终 Gate：关键验证已补齐，SYSTEM-FINALIZE-1 COMPLETED；创建 versions/v1.0-final/README.md，可作为课程设计软件 v1.0 提交，不自动开始新阶段。启动方式见 RESUME；正式课程材料和教师验收不由本次软件收口替代。
- Git：main，保留前轮 16 个已跟踪文件修改；本次仅更新 CURRENT_CONTEXT、RESUME 并新增 v1.0-final/README.md。前轮 diff --check、冲突/凭据/生成物检查通过，本次再核对文档差异与修改范围。课程设计/.DS_Store 仍为既有跟踪文件，未改动；Git writes = NONE。

本阶段前轮实际修改文件（本次仅再同步其中两份上下文，另新增 v1.0 归档）：

```text
frontend/src/utils/workbenchState.js
frontend/src/utils/workbenchState.test.js
课程设计/AGENTS.md
课程设计/README.md
课程设计/00-项目总览/README.md
课程设计/00-项目总览/项目导航.md
课程设计/00-项目总览/项目当前状态.md
课程设计/01-立项与需求/04-功能需求.md
课程设计/01-立项与需求/09-需求冻结确认.md
课程设计/02-系统设计/01-总体架构设计.md
课程设计/02-系统设计/02-功能模块设计.md
课程设计/02-系统设计/03-业务流程设计.md
课程设计/02-系统设计/04-接口设计.md
课程设计/05-测试与验收/02-最终验收方案.md
课程设计/docs/context/CURRENT_CONTEXT.md
课程设计/docs/context/RESUME.md
```

以下为 DOC-V2-INIT-PLAN 的历史记录，不代表当前 Git 状态：当时工作区干净，但 30 个文档/Canvas 中保留已提交冲突标记；该轮清理冲突并修订初始化计划 V2。该轮核对 main 与本地 origin/main 均为 1740f78e0b7751ca65e8cbb803c0be6065dba88a，未 fetch。Git 暂存、commit、push、pull、merge、tag 等写操作始终由用户通过 GitHub Desktop 完成。

## 2. 冻结方案

- 前端：Vue 3、Vite、JavaScript、Element Plus、ECharts、Axios、Vue Router。
- 后端：Java 17、Spring Boot 2.7.18、MyBatis-Plus、MyBatis XML、Maven 3.9.16；MySQL 8.0.16+。
- 测试：JUnit 5、Spring Boot Test、Postman / Apifox。
- 四页：/weather（/ 重定向）、/analysis、/comparison、/management。
- 工作台地图 P0，ECharts Map + 静态 GeoJSON；16 市，默认 ECMWF + T2M + 2026-09-07 08:00～14:00，批量加载、页面实例内存缓存、时间轴本地过滤、当前时次动态色标。
- 趋势双要素双图与四项 Service 统计，无明细表；双模型对比只做视觉比较；管理一个 Tabs 页面四类完整 CRUD。
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

已完成：本机环境与工程骨架、PROJECT-INIT-1、DB-INIT-1；三个展示查询与 Management 16 / 16 CRUD API、四页面阶段验收，以及 SYSTEM-FINALIZE-1 系统级最终回归、必要修复与当前文档一致性同步。扩展数据整理、课程正式验收和展示材料不属于本次软件收口，不自动开展。

## 5. 长期边界与协作

Monorepo：根 frontend/、backend/、database/ 保存当前实现，课程设计/ 唯一文档，versions/ 阶段说明/截图/测试/Git 信息，不复制源码、不另建重复 docs/。main + feat/fix/docs/test 短分支，真实历史由 commit/tag 保存，不伪造版本信息。

不引入登录/JWT、Redis、TDengine、ERA5、NetCDF 在线处理、实时采集、复杂 GIS、风场/高空场、模型评分训练、微服务、DDD 或自动交易。Windy 仅交互思想参考，资产和代码不复制。默认不暂存、不提交、不推送，不覆盖用户无关修改。

## 6. 下一阶段

SYSTEM-FINALIZE-1 已完成并归档 v1.0；提交由用户通过 GitHub Desktop 操作，不自动创建新阶段。以下保留 FE-MANAGEMENT-CRUD-1 COMPLETED / v0.13 历史收口证据：

- /management：城市、预报模型、气象要素、预报记录四 Tab；各自列表、新增、编辑、删除、校验与冲突提示，引用保护交互，Loading/Empty/Error/Retry，独立 Draft、防重复提交、旧响应及卸载保护；字典变更使记录元数据和对应下拉失效并在下次使用前刷新。沿用既有 16 API、Axios；无分页、动态 CRUD 框架或业务扩展。
- 前轮实际执行：四资源真实 CRUD、描述清空 null 后显示 --、数字 ID/本地时间、PRECIP 负值拦截与 0.00、重复冲突、字典刷新通过。临时要素 #8 的删除、编码及单位修改均独立复核 HTTP/code 409，UI 保留原行和草稿；临时记录 #202、要素 #8 已清理并确认 ID 不存在。
- 最近自动验证：原有 71 + 新增 71 = 142 passed / 0 failed / 0 skipped；保留 node --test，新增授权的 Vitest/Vue Test Utils/DOM 组件测试链路；npm run build PASS。仅引用此前实际结果，不冒充本轮重跑。
- USER_MANUAL_CONFIRMED：浏览器限速下 GET Loading、指定 GET 受控网络失败时的局部错误、解除阻止后的页面内 Retry、记录依赖字典失败与恢复、四资源 Empty、1920×1080 完整视觉、撤销模拟后的真实列表和正常 Console 均通过。受阻请求不等同后端返回 HTTP 500；Empty 证据为 BROWSER_SIMULATED_EMPTY，不是真实 MySQL 空表。1366×768 沿用前轮列表与弹窗实测。
- 数据数量 16 / 2 / 2 / 192 采用前轮真实 GET；数量恢复不证明全库所有字段逐值相同。/weather 地图、Timeline、济南详情，/analysis 双图及四统计，/comparison 双模型曲线沿用前轮有效回归，不冒充本轮浏览器重测。
- 非阻塞事项未修复：共享包 >500 kB、既有被跟踪的课程设计/.DS_Store、要素单位冲突沿用编码冲突文案。旧文档中的工程未初始化、Comparison PRECIP 柱状表现等与已批准实现的差异，留待一致性审查，本轮不改旧文档。
- 归档：[v0.13-fe-management-crud](../../../versions/v0.13-fe-management-crud/README.md)。本轮只读 Git/源码证据核对及三份收口文档同步，不修改业务、测试、依赖或历史归档，不执行数据库写入。Git writes = NONE。

以下为 v0.12 后端已完成基线的历史证据。BE-MANAGEMENT-CRUD-1 COMPLETED（2026-09-09）：

- 依据用户提供的本机真实 MySQL Runtime 验收结果：四类 GET、四类 POST/PUT/DELETE、重复城市编码与记录组合 409、被引用字典删除 409、不存在城市删除 404、PRECIP 负数 400 与真实 0.00 返回 200，均 PASS。临时 Runtime 数据已清理，Initial / Final DB 均为 16 / 2 / 2 / 192。
- Controller → Service → Mapper → MySQL；专用写 DTO，新增 ForecastRecord Entity/Service/Controller，既有 ForecastRecordMapper 扩展 BaseMapper，无第二套 Mapper，Workbench/Trend/Comparison 自定义查询保持。GET /api/forecast-records 全量、id ASC、冻结扩展字段，无分页或高级筛选。
- 已实现必填、长度、坐标、精度、严格时间、安全 ID、关联存在与 PRECIP 非负校验；沿用 ApiResponse/BusinessException/GlobalExceptionHandler 的 400/404/409/500。Service 主动判重、数据库 UNIQUE/FK 兜底；被引用编码及要素 unit 不能改变，无级联/逻辑删除。写操作使用事务，集成测试事务回滚。
- 实现轮：City CRUD 24、Model CRUD 20、Element CRUD 22、Record CRUD 28、Management Binding 3 PASS；全部非数据库测试 273 PASS / 0 failures / 0 errors / 0 skipped。用户本机确认 ManagementCrudIntegrationTests、Full mvn test、Full mvn package、Spring Boot repackage PASS；未提供完整 Maven Tests run 数量，不推算。收口轮未重跑 Maven 或 HTTP。
- 用户真实回归：Dictionary、Workbench、Trend、Comparison PASS。Schema / Seed / Index UNCHANGED，仍为四表；本阶段未实现 FE Management。
- 归档：[v0.12-be-management-crud](../../../versions/v0.12-be-management-crud/README.md)。收口仅改两份上下文与该归档，不改业务或历史归档，不记录密码或虚构 commit/tag/push。课程设计/.DS_Store 为既有独立非阻塞清理项，未移除。Git writes = NONE。

以下为前置阶段历史证据，不代替上述当前状态。

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

前置 BE-TREND-1 COMPLETED（2026-09-08，以下为该阶段记录）：

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

前置 FE-TREND-1 COMPLETED（2026-09-08，以下为该阶段记录）：

- 开始 Gate：feat/fe-trend、工作区干净，HEAD 与本地 origin/main 均为 4a528eee9866e752fb3a11dfe2e3184be0e435b1；未 fetch，未执行 Git 写操作。前置 BE-TREND-1 COMPLETED；本阶段后端与数据库未修改，沿用真实 134 tests PASS、Package PASS、Trend Runtime PASS。
- /analysis 复用 TrendAnalysis.vue、现有 App Shell 与 Axios /api；TrendFilterBar、TrendStatistics、通用 TrendChart 实现深色气象工作台风格的城市/模型/时间范围/查询按钮、T2M 折线图、PRECIP 柱状图、四项统计卡，以及 Loading、Empty、Error、Retry、Race Protection 和响应式布局。未新增依赖或 Cache。
- 并行读取 GET /api/cities 与 GET /api/forecast-models，按 JINAN/ECMWF 编码选择默认项，缺省时使用可用核心选项；复用集中定义的 2026-09-07 08:00:00～14:00:00 范围。初始化自动查询一次 GET /api/weather/trend，之后只在点击查询时请求，编辑筛选不自动请求；不转换 UTC。
- temperature/precipitation 各自独立使用 forecastTime/value，不按数组下标强制对齐，不补零。四项 statistics 只读取 temperatureMax/temperatureMin/temperatureAvg/precipitationTotal，不在前端重算；真实 0 显示 0.00，null/undefined 显示 --。页面提示统计基于返回样本。两个 ECharts 实例分别 init、setOption、resize，卸载 dispose 并释放监听/观察器。
- requestVersion 废弃旧成功、旧失败和卸载后的响应；Retry 使用当前四项筛选，不重置用户条件。提交新查询清除旧结果；筛选已修改但未提交时，页面明确提示并保留已查询结果的元数据。
- 实现轮 TDD：原 20 项通过、新 25 项因功能缺失失败；实现后 npm test 45 passed / 0 failed / 0 skipped，包含 API 参数、默认编码、图表真实 ECharts SVG 渲染与销毁、零值/空值、双系列不同时次、四统计映射、状态、Retry 与竞态。独立代码审查通过，无 Critical/Important/Minor。npm run build PASS（2125 modules）；主 JS 1345.66 kB/gzip 447.95 kB，大 chunk 为 KNOWN NON-BLOCKING MINOR。本轮仅文档收口，未重跑测试或构建。
- 最终 Runtime 证据来源：用户本次明确确认真实 Spring Boot + MySQL 人工浏览器验收全部通过，不冒充 Codex 本轮直接采集；此前 Codex 后端离线及截图裁切限制不再作为未完成项，但不声明服务当前持续在线。
- Default PASS：济南 + ECMWF，默认 2026-09-07 08:00～14:00；T2M 19.00/20.20/21.10℃，PRECIP 0.00/0.40/0.20 mm；temperatureMax/temperatureMin/temperatureAvg/precipitationTotal 为 21.10/19.00/20.10/0.60。
- City Switch PASS：JINAN → QINGDAO 两图与四统计刷新，无济南残留；Model Switch PASS：ECMWF → NOAA 两图与统计刷新。Loading 可见、框架保持、无白屏；无数据日期 HTTP 200，页面 Empty、四统计 --，无 Error/NaN/undefined。
- Error / Retry PASS：后端断开进入 Error，恢复后重试成功且保留筛选。Race PASS：快速模型/筛选切换，最终状态对应最后一次请求，无旧响应覆盖。
- Responsive：1366×768 PASS、1920×1080 PASS，无横向溢出，筛选、统计卡及图表布局正常。Console PASS：0 Uncaught Error、0 Unhandled Promise、0 Vue warning、0 ECharts disposed error。/weather regression PASS：山东地图、ECMWF/T2M、Timeline、City Detail 正常，Trend 样式未污染 Workbench；以上均来源于用户本次人工验收。
- 最终收口仅更新 CURRENT_CONTEXT、RESUME 并创建 [v0.9-fe-trend](../../../versions/v0.9-fe-trend/README.md)，不复制源码、不改历史归档、不修改业务代码。课程设计/.DS_Store 仍为独立非阻塞清理项，未自行移除；凭据不入库。
- Gate：READY FOR NEXT PHASE。下一建议先审计 Comparison Backend，不直接假设 /api/weather/comparison 已实现；不自动开始审计、Comparison 或 Management。Git 暂存、commit、tag、push 仍由用户通过 GitHub Desktop 操作，本轮 Git writes：NONE。

前置 FE-COMPARISON-1 COMPLETED（2026-09-09，以下为该阶段记录）：

- /comparison 复用 Axios、日期控件及集中演示范围，按编码默认 JINAN/T2M、2026-09-07 08:00:00～14:00:00。初始化字典完成后查询一次，之后按钮触发。暗色筛选栏、模型摘要及主图；ECMWF/NOAA 的 T2M、PRECIP 均为双折线，时间并集 ASC 对齐，缺测映射 null、真实 0.00 保留。Loading/Empty/Error/Retry/requestVersion Race Protection、resize/dispose 已实现；no cache、no metrics，不含 RMSE/MAE/Bias/Accuracy。
- 实现轮先失败测试再实现：原 45 + 新 26 = 71 passed / 0 failed / 0 skipped；npm run build PASS，共享包 >500kB warning 为 NON-BLOCKING。收口轮不重跑，不把历史执行写为本轮执行。
- 用户本次真实浏览器验收确认全部 PASS：Default T2M、PRECIP、City Switch、Empty、Loading、Error、Retry、Race、1366×768、1920×1080、Console、/weather Regression、/analysis Regression。济南 08/11/14：T2M ECMWF 19.00/20.20/21.10℃、NOAA 18.40/19.60/20.50℃；PRECIP ECMWF 0.00/0.40/0.20 mm、NOAA 0.10/0.60/0.30 mm。证据来自用户确认，不冒充本轮工具实测，不声明服务持续在线。
- DB-CONNECTION-CONFIG-FIX COMPLETED：此前 Public Key Retrieval is not allowed；application.yml JDBC URL 仅追加 allowPublicKeyRetrieval=true，保留 useSSL=false 及 DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD 环境变量机制。未改账号、密码、Schema/Seed。用户确认修复后三个字典 GET 均 200，三个核心页面恢复。此前修复轮 mvn test/package 因 Maven Central 访问被拒而未进入测试，不记为 PASS；本轮未重跑。
- 归档：[v0.11-fe-comparison](../../../versions/v0.11-fe-comparison/README.md)。本轮仅同步两份上下文与归档，未改业务或历史归档；既有课程设计/.DS_Store 为独立非阻塞清理项。Git writes = NONE。下一阶段 MANAGEMENT-BACKEND-AUDIT，需独立只读 Git Gate，未开始审计或实现。

前置 BE-COMPARISON-1 COMPLETED（2026-09-08，以下为该阶段记录）：

- 开始 Gate：feat/be-comparison，工作区 clean，HEAD 与本地 origin/main 均为 f15832089ec3f2a8431769bbc88567443e9227fd；未 fetch、未执行 Git 写操作。沿用已完成 Comparison Audit 的 NO_SCHEMA_CHANGE_REQUIRED、NO_NEW_INDEX_REQUIRED、SEED_READY_FOR_COMPARISON。
- Endpoint：GET /api/weather/comparison；cityId、elementId、startTime、endTime 全必填，严格本地业务时间与闭区间，起止相等合法。调用链为 WeatherQueryController → WeatherQueryService → ForecastRecordMapper → XML → MySQL，ComparisonRecordRow 投影及 ComparisonVO 响应。
- data：cityId、cityName、element、series；element 含 id/elementCode/elementName/unit；series 每项含 modelId/modelCode/modelName/values，点含 forecastTime/value。按 modelCode 解析 ECMWF/NOAA 实际 ID，不硬编码 1/2、不依赖字典顺序；响应固定 ECMWF first、NOAA second，不返回 statistics 或顶层 times。
- selectComparisonData 使用 forecast_record JOIN forecast_model；绑定 cityId/elementId/modelIds/startTime/endTime，foreach 与空集合保护。各系列由 Service 再按 forecastTime 升序；缺测不补 0 或 null 点，BigDecimal 原值及真实零保留。Empty HTTP/code 200 保留城市、要素和两个空 values 系列。非法参数/非支持范围 400，城市或要素不存在 404，任一核心模型缺失 500。
- 实现轮 TDD：Service 20、Controller 32、Binding 3 项先失败再通过；连同原有非数据库回归共 176 passed / 0 failures / 0 errors / 0 skipped。test-compile 与限定非数据库测试的 package/repackage PASS；独立只读代码审查无 Critical/Important/Minor。
- 最终 Runtime 依据用户本次提供的真实 Spring Boot + MySQL 验收，不冒充 Codex 收口轮重跑：ComparisonMapperIntegrationTests 4 passed；完整 mvn test 193 passed；mvn package 193 passed、Spring Boot repackage PASS；均 0 failures / 0 errors / 0 skipped、BUILD SUCCESS。193 = 既有 134 + Comparison 59。Application Startup PASS：Tomcat 8080、Started WeatherApplication；不声明服务持续在线。
- 济南、2026-09-07 08:00～14:00：T2M HTTP/code 200，两个系列各 3 点；ECMWF 19.00/20.20/21.10℃，NOAA 18.40/19.60/20.50℃。PRECIP HTTP/code 200、mm；ECMWF 0.00/0.40/0.20，NOAA 0.10/0.60/0.30，真实零保留，均 PASS。数据为固定合成工程演示数据，不是真实模型预报。
- Runtime 边界 PASS：时间倒置 HTTP 400，cityId=999999 HTTP 404，elementId=999999 HTTP 404。2026-09-08 08:00～14:00 Empty HTTP/code 200，仍为 ECMWF/NOAA 两个 series 且 values=[]，不能改成 series=[]。
- 四项集成测试只读 SELECT，每项前后核验 city/forecast_model/weather_element/forecast_record 为 16/2/2/192；用户确认事务回滚及完整性保持。Schema/Seed unchanged，无新增表或索引。完整 193 项覆盖 Comparison、Trend、Workbench、Dictionary、Database Connection；既有业务行为不变。
- 本阶段 frontend 未修改；实现轮 npm test 45 passed、npm run build PASS，大 chunk 为既有非阻塞提示。收口轮只同步两份上下文并创建 [v0.10-be-comparison](../../../versions/v0.10-be-comparison/README.md)，未重跑 Maven、前端构建或 HTTP，不复制源码、不改历史归档。
- 凭据仅由本机环境变量注入，不索取或记录密码。课程设计/.DS_Store 仍为独立非阻塞清理项，不自行移除。Git writes：NONE，暂存/提交/tag/推送由用户操作。Gate：READY FOR FE-COMPARISON-1，未开始 FE-COMPARISON、Management 或 CRUD。

恢复入口：[RESUME.md](RESUME.md)。[既有任务模板（历史格式参考）](../development/TASK_TEMPLATE.md) 的旧三模块、六城举例和记录接口示例不作为当前规范；编写新任务时必须按本页链接的 V2 需求/API 填写。模板正文不在本轮允许修改范围内，保持原文。仅阶段、决策、实际完成项或下一优先级变化时更新上下文。
