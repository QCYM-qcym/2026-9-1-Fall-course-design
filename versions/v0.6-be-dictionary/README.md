# v0.6 Backend Dictionary

## 版本目标与状态

BE-DICTIONARY-1 COMPLETED，验收日期 2026-09-07。

为前端 Weather Workbench 联调提供三个只读字典接口，完成 Controller → Service → MyBatis-Plus BaseMapper → MySQL 调用链。Gate：READY FOR FE-WORKBENCH-1，不自动开始下一阶段。

## API 与实现

| GET | 成功 data 字段 | 真实数量 |
| --- | --- | ---: |
| /api/cities | id、cityCode、cityName、longitude、latitude | 16 |
| /api/forecast-models | id、modelCode、modelName、description | 2 |
| /api/weather-elements | id、elementCode、elementName、unit | 2 |

遵循 [API V2](../../课程设计/02-系统设计/04-接口设计.md)，三个接口均无参数、不分页，返回全部字典，显式按 id ASC 排序，不按 Workbench 核心编码过滤。成功包装为 code=200、message=success、data 数组；空表返回 []，description 保留 string/null，坐标为 BigDecimal 对应的 JSON number。

Entity 字段与表及 API 一致，直接返回最小 Entity；使用 @TableName、@TableId(AUTO) 和既有下划线转驼峰映射，不新建 DTO/VO、XML 或依赖。复用 ApiResponse 与统一异常处理。

新增主代码：

- [CityController](../../backend/src/main/java/com/shandong/weather/controller/CityController.java)
- [ForecastModelController](../../backend/src/main/java/com/shandong/weather/controller/ForecastModelController.java)
- [WeatherElementController](../../backend/src/main/java/com/shandong/weather/controller/WeatherElementController.java)
- [CityService](../../backend/src/main/java/com/shandong/weather/service/CityService.java)
- [ForecastModelService](../../backend/src/main/java/com/shandong/weather/service/ForecastModelService.java)
- [WeatherElementService](../../backend/src/main/java/com/shandong/weather/service/WeatherElementService.java)
- [CityMapper](../../backend/src/main/java/com/shandong/weather/mapper/CityMapper.java)
- [ForecastModelMapper](../../backend/src/main/java/com/shandong/weather/mapper/ForecastModelMapper.java)
- [WeatherElementMapper](../../backend/src/main/java/com/shandong/weather/mapper/WeatherElementMapper.java)
- [City](../../backend/src/main/java/com/shandong/weather/entity/City.java)
- [ForecastModel](../../backend/src/main/java/com/shandong/weather/entity/ForecastModel.java)
- [WeatherElement](../../backend/src/main/java/com/shandong/weather/entity/WeatherElement.java)

新增测试：

- [CityServiceTest](../../backend/src/test/java/com/shandong/weather/CityServiceTest.java)
- [ForecastModelServiceTest](../../backend/src/test/java/com/shandong/weather/ForecastModelServiceTest.java)
- [WeatherElementServiceTest](../../backend/src/test/java/com/shandong/weather/WeatherElementServiceTest.java)
- [DictionaryControllerTest](../../backend/src/test/java/com/shandong/weather/DictionaryControllerTest.java)
- [DictionaryMapperBindingTest](../../backend/src/test/java/com/shandong/weather/DictionaryMapperBindingTest.java)
- [DictionaryMapperIntegrationTests](../../backend/src/test/java/com/shandong/weather/DictionaryMapperIntegrationTests.java)
- [DictionaryTestData](../../backend/src/test/java/com/shandong/weather/DictionaryTestData.java)

仅 CURRENT_CONTEXT 和 RESUME 同步当前状态；不复制源码到版本目录。

## 测试与构建证据

执行方式：Codex 实现、局部验证；用户在已配置本机数据库环境变量的 PowerShell 中完成真实数据库及完整构建验收。本记录摘录结果，不复制完整控制台日志或凭据。

TDD：实现前三个路由测试均实际返回 404、期望 200；实现后通过。Service 测试检查全量查询、显式排序与空列表；MockMvc 使用真实 Controller/Service、只替换 Mapper，验证精确字段、非核心编码不被过滤、description:null、中文单位、空数组和安全 500 响应。Mapper 绑定测试加载真实注册与生成 SQL，不连接数据库。

| 测试类 | 完整测试数量 | 结果 |
| --- | ---: | --- |
| CityServiceTest | 3 | PASS |
| ForecastModelServiceTest | 3 | PASS |
| WeatherElementServiceTest | 3 | PASS |
| DictionaryControllerTest | 9 | PASS |
| DictionaryMapperBindingTest | 3 | PASS |
| DictionaryMapperIntegrationTests | 3 | PASS |
| DatabaseConnectionTests | 1 | PASS |
| WeatherApplicationTests | 1 | PASS |
| WeatherQueryControllerTest | 29 | PASS |
| WeatherQueryServiceTest | 14 | PASS |
| WorkbenchMapperBindingTest | 2 | PASS |
| WorkbenchMapperIntegrationTests | 5 | PASS |

Codex 非数据库回归：67 passed / 0 failures / 0 errors / 0 skipped；mvn test-compile PASS，全部测试源码编译通过，Java 17 字节码 major version 61。原有 52 项测试未删除或修改，本阶段新增 24 项，总计 76 项。

用户提供的 2026-09-07 真实运行日志（Asia/Shanghai）：

| 命令 | tests/failures/errors/skipped | 完成时间 | 构建 |
| --- | --- | --- | --- |
| mvn "-Dtest=DictionaryMapperIntegrationTests" test | 3/0/0/0 | 20:50:41 | BUILD SUCCESS |
| mvn test | 76/0/0/0 | 20:50:47 | BUILD SUCCESS |
| mvn package | 76/0/0/0 | 20:50:52 | BUILD SUCCESS，Spring Boot repackage 成功 |

前端 npm run build 在最终收口再次 PASS；既有 1,303.21 kB chunk 提示为 KNOWN NON-BLOCKING MINOR，未调整前端。

## 数据库与 HTTP 验收

真实集成测试通过 Spring Service → MyBatis-Plus Mapper → MySQL 查询 shandong_weather，无 Mock，测试只执行 SELECT。验证字典数量 16/2/2、编码唯一、id 升序、济南/青岛名称与坐标、ECMWF/NOAA 名称和说明、T2M/PRECIP 名称及 ℃/mm 单位；每项前后四表数量断言保持 16/2/2/192。

用户启动应用后，Codex 已实际执行只读 HTTP 请求：

| 场景 | HTTP/code | 结果 |
| --- | --- | --- |
| GET /api/cities | 200/200 | 16 市，字段含经纬度，id 升序 |
| GET /api/forecast-models | 200/200 | ECMWF、NOAA，含 description |
| GET /api/weather-elements | 200/200 | T2M、2 米气温、℃；PRECIP、降水量、mm |
| Workbench ECMWF/T2M，2026-09-07 08:00～14:00 | 200/200 | 3 times、48 records，每时次 16 个不同城市 |

空表及 description:null 场景由 MockMvc 验证，未清空真实数据库制造测试数据。最终收口再次请求时 8080 无法连接；上表为此前同一实现的成功验收证据，不声明后端在归档时仍持续运行。需要访问时由用户在环境变量齐备的终端启动。

## 技术、安全与范围

Java 17.0.11、Spring Boot 2.7.18、Maven 3.9.16、MyBatis-Plus 3.5.7、Connector/J 8.0.33；MySQL 8.0.46 为既有实际运行基线。项目源码和构建编码 UTF-8。

NO_SCHEMA_CHANGE_REQUIRED。Schema、Seed、索引、Workbench 契约/SQL/Service、依赖和前端源码未修改；历史 v0.2～v0.5 归档未修改。数据库仍为四表 16/2/2/192，工程预报数据仍为固定合成样例，不代表真实气象预报。

开发账号按用户决策使用本机 root，通过本机环境变量注入；未索取、读取或写入真实密码，未创建/跟踪 .env 或 .mylogin.cnf。未引入缓存、分页或额外业务实体。独立只读代码审查未发现需修正问题。

POST/PUT/DELETE、Trend、Comparison、GeoJSON、地图及前端业务尚未实现，属于后续范围，不算本阶段缺陷。

## Git 与下一阶段

- Branch：feat/be-dictionary。
- 开发起点 HEAD / 本地 origin/main：35aa067d13a0e4e9d5dad073c406f6d94a216a80；此为前置基线，不是本版本完成提交。
- 本版本 Commit：归档时尚未提交，由用户通过 GitHub Desktop 操作。
- Tag / Push：Codex 未创建或执行，不虚构人工后续状态。
- 下一推荐任务：FE-WORKBENCH-1，须由用户另行授权，不自动开始。
