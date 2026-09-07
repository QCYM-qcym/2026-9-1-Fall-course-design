# v0.5 Backend Workbench

## 阶段目标与状态

BE-WORKBENCH-1 COMPLETED，验收日期 2026-09-07。

实现第一个核心业务接口 GET /api/weather/workbench，完成 HTTP → Controller → Service → MyBatis Mapper/XML → MySQL 四表 JOIN → ApiResponse 的只读调用链。

Gate：READY FOR NEXT PHASE。

## 实现文件

主代码：

- [WeatherQueryController](../../backend/src/main/java/com/shandong/weather/controller/WeatherQueryController.java)：接收参数、严格解析本地时间、调用 Service。
- [WeatherQueryService](../../backend/src/main/java/com/shandong/weather/service/WeatherQueryService.java)：值与关联校验、核心范围校验、响应组装和 times 去重升序。
- [ForecastRecordMapper](../../backend/src/main/java/com/shandong/weather/mapper/ForecastRecordMapper.java) 与 [Mapper XML](../../backend/src/main/resources/mapper/ForecastRecordMapper.xml)：字典元数据查询和四表 JOIN。
- [WorkbenchRecordRow](../../backend/src/main/java/com/shandong/weather/mapper/WorkbenchRecordRow.java)、[WorkbenchModelRow](../../backend/src/main/java/com/shandong/weather/mapper/WorkbenchModelRow.java)、[WorkbenchElementRow](../../backend/src/main/java/com/shandong/weather/mapper/WorkbenchElementRow.java)：内部查询投影。
- [WorkbenchVO](../../backend/src/main/java/com/shandong/weather/vo/WorkbenchVO.java)：公开响应，内部查询行不直接暴露。
- [GlobalExceptionHandler](../../backend/src/main/java/com/shandong/weather/common/GlobalExceptionHandler.java)：补齐缺参与类型错误的统一 400 响应。
- [application.yml](../../backend/src/main/resources/application.yml)：配置冻结的核心城市、模型、要素编码范围；ID、名称和单位仍从数据库读取。

测试：

- [WeatherQueryServiceTest](../../backend/src/test/java/com/shandong/weather/WeatherQueryServiceTest.java)
- [WeatherQueryControllerTest](../../backend/src/test/java/com/shandong/weather/WeatherQueryControllerTest.java)
- [WorkbenchMapperBindingTest](../../backend/src/test/java/com/shandong/weather/WorkbenchMapperBindingTest.java)
- [WorkbenchMapperIntegrationTests](../../backend/src/test/java/com/shandong/weather/WorkbenchMapperIntegrationTests.java)
- [WorkbenchTestData](../../backend/src/test/java/com/shandong/weather/WorkbenchTestData.java)

既有 DatabaseConnectionTests 和 WeatherApplicationTests 保留，未弱化断言；CURRENT_CONTEXT、RESUME 同步当前阶段。不复制源码到版本目录。

## API 与 SQL

唯一业务接口：GET /api/weather/workbench。modelId、elementId、startTime、endTime 全部必填；严格使用 yyyy-MM-dd HH:mm:ss 本地业务时间，范围为闭区间。

响应遵循 [API V2 第 6 节](../../课程设计/02-系统设计/04-接口设计.md)：

- model：id、modelCode、modelName。
- element：id、elementCode、elementName、unit。
- times：去重升序时间字符串。
- records：cityId、cityName、forecastTime、value；value 保持 BigDecimal 数值语义。
- 无记录仍返回 200、真实字典元数据及空数组；非法参数/非核心展示字典为 400，不存在关联为 404。

selectWorkbenchData 使用 forecast_record JOIN city JOIN forecast_model JOIN weather_element，按模型、要素、时间范围和城市编码集合过滤，按 forecast_time ASC、city_id ASC 排序。全部参数使用 #{} 绑定；空城市集合匹配零行。

两次字典元数据查询支持存在性判断和无记录时的元数据返回，不循环逐行查库。未修改 Schema、Seed、索引或 pom.xml，NO_SCHEMA_CHANGE_REQUIRED。

## 验证来源与结果

执行模式为 Codex 实现与局部验证、用户本机真实数据库验收。本记录摘录结果，不保存含个人路径的完整控制台日志或任何凭据。

Codex 在实现前运行缺参失败测试，实际 404、期望 400；实现后运行不依赖数据库凭据的测试：

| 测试类 | 数量 | 结果 |
| --- | ---: | --- |
| WeatherQueryServiceTest | 14 | PASS |
| WeatherQueryControllerTest | 29 | PASS |
| WorkbenchMapperBindingTest | 2 | PASS |
| WeatherApplicationTests | 1 | PASS |

局部合计 46/0/0/0；mvn test-compile PASS，包含真实数据库集成测试的源码编译；前端 npm run build PASS，既有大 chunk 提示为非阻塞 Minor。真实 Mapper 注册后未再出现 No MyBatis mapper 提示。

用户提供的本机 PowerShell 真实日志：

| 命令 | 测试结果（tests/failures/errors/skipped） | 完成时间（Asia/Shanghai） | 构建 |
| --- | --- | --- | --- |
| mvn "-Dtest=WorkbenchMapperIntegrationTests" test | 5/0/0/0 | 2026-09-07 16:03:48 | BUILD SUCCESS |
| mvn test | 52/0/0/0 | 2026-09-07 16:03:53 | BUILD SUCCESS |
| mvn package | 52/0/0/0 | 2026-09-07 16:04:02 | BUILD SUCCESS，repackage 成功 |

完整 52 项由上述 46 项、DatabaseConnectionTests 1 项、WorkbenchMapperIntegrationTests 5 项组成，无跳过。真实集成测试根据数据库编码查 ID，不 Mock Mapper；只读验证 NOAA、两模型 PRECIP 非负、闭区间、空结果、城市过滤及四表数量。

## 真实 HTTP 验收

用户启动应用后，Codex 对本机服务进行了只读 HTTP 请求：

| 场景 | HTTP / code | 数据结果 |
| --- | --- | --- |
| ECMWF + T2M，09-07 08:00～14:00 | 200 / 200 | 48 records、3 times，每个时次 16 市 |
| NOAA + T2M，同范围 | 200 / 200 | 48 records、3 times |
| ECMWF + PRECIP，同范围 | 200 / 200 | 48 records、3 times，负值数量 0 |
| startTime > endTime | 400 / 400 | 统一错误响应 |
| 缺少 modelId | 400 / 400 | 统一错误响应 |
| 不存在 modelId | 404 / 404 | 统一错误响应 |
| 2027 年无记录范围 | 200 / 200 | 保留模型/要素元数据，times=[]、records=[] |

成功 message 为 success；模型元数据为 id=1、ECMWF，温度要素为 id=1、T2M、2 米气温、℃。三个时次为 2026-09-07 08:00:00、11:00:00、14:00:00。

ECMWF/T2M 首条：济南 cityId=1，08:00，19.00；末条：菏泽 cityId=16，14:00，23.20。济南三个时次值为 19.00、20.20、21.10，均为固定合成工程样例，不代表真实预报。

## 数据库、安全与限制

测试通过真实 Spring DataSource/HikariCP/MyBatis 访问 MySQL 8.0.46 的 shandong_weather。集成测试每项结束时断言 city=16、forecast_model=2、weather_element=2、forecast_record=192；全部通过。业务 SQL 和集成测试只读，不修改数据库。

开发账号按用户决策使用本机 root，用户名与密码仅通过本机环境变量注入。未创建 .env，未跟踪 .mylogin.cnf，未写入明文密码。

本版本仅实现 Workbench 后端。三类字典 GET、CRUD、Trend、Comparison、GeoJSON、地图及其他前端业务尚未实现；不引入缓存、分页、新依赖或额外架构层。前端大 chunk 提示暂不处理。

## Git 与下一阶段

- Branch：feat/be-workbench。
- 开发起点 HEAD：b39cec6883fd0d3a9545a4d6305d94b30a4ad27f；这是前置基线，不是本版本完成提交。
- 本版本 Commit：尚未提交，由用户通过 GitHub Desktop 完成。
- 本版本 Tag / Push：Codex 未创建或执行，不声明人工后续状态。
- 完成后由用户检查 Git 差异并提交；下一阶段建议准备前端 Workbench 联调所需的字典读取与地图资源，具体独立任务由用户另行确定。本轮不自动开始。
