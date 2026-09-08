# v0.10 — BE-COMPARISON-1

> 收口日期：2026-09-08。BE-COMPARISON-1 COMPLETED。
> Gate：READY FOR FE-COMPARISON-1。

## 任务目标与范围

完成同一城市、同一要素、同一时间范围内 ECMWF/NOAA 双模型预报趋势查询后端。仅阶段元数据留档，不复制源码；不实现 FE-COMPARISON、Management、CRUD、评分或误差评估。

契约依据：[API V2](../../课程设计/02-系统设计/04-接口设计.md)。状态入口：[CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)、[RESUME](../../课程设计/docs/context/RESUME.md)。

## Endpoint、Params 与 Response

- GET /api/weather/comparison。
- cityId、elementId、startTime、endTime 全必填；不传 modelId/modelIds。
- 严格 yyyy-MM-dd HH:mm:ss 本地业务时间，闭区间，起止相等合法。
- ApiResponse 包装 code/message/data；data 仅 cityId、cityName、element、series。
- element：id、elementCode、elementName、unit。
- series 固定两项：ECMWF first、NOAA second；每项 modelId、modelCode、modelName、values。
- values 每点 forecastTime、value；数值为 BigDecimal，不转 double。不返回 Trend statistics、顶层 elementId 或 times。

## 架构、模型解析与 SQL

WeatherQueryController → WeatherQueryService → ForecastRecordMapper → ForecastRecordMapper.xml → MySQL；ComparisonRecordRow 为 modelCode/LocalDateTime forecastTime/BigDecimal value 投影，ComparisonVO 使用嵌套 ElementInfo、ModelSeries、SeriesPoint。

复用城市、要素、模型 Mapper、严格时间解析、安全 ID 校验及统一异常。通过 forecast_model.model_code 查询 ECMWF/NOAA，解析实际 ID，不硬编码模型 1/2，也不依赖字典返回顺序。任一核心模型缺失为配置错误 500，不静默返回单模型。

selectComparisonData 使用 forecast_record fr JOIN forecast_model fm，投影 fm.model_code、fr.forecast_time、fr.value。WHERE 绑定 cityId、elementId、实际 modelIds 集合与 startTime/endTime；使用 #{}、foreach，null/空模型集合以 AND 1 = 0 保护。ORDER BY fm.model_code ASC、fr.forecast_time ASC；Service 再分别排序并固定两模型顺序。Workbench/Trend 原 SQL 和业务行为不改动。

## Missing Data、Zero 与 Empty

- 各模型独立返回实际点，不补 0、不添加 null placeholder，前端以后按 forecastTime 对齐。
- 真实降水 0.00 保留，不误判缺失。
- 单模型无记录仍保留该系列；合法关联但两模型无数据时 HTTP/code 200，保留城市、要素及 ECMWF/NOAA 两个 values=[]，不能变成 series=[] 或 404。
- 缺参、非法 ID/日期、倒置范围、存在但不支持的城市/要素为 400；请求城市/要素不存在为 404；服务端异常安全包装，不泄漏底层详情。

## Tests 与证据来源

实现轮 Codex 按 TDD 观察预期失败后实现：Service 20、Controller 32、Binding 3 共 55 项通过；加原有非数据库 121 项，共 176 passed / 0 failures / 0 errors / 0 skipped。mvn test-compile 与限定非数据库测试的 package/repackage PASS；独立只读审查无 Critical/Important/Minor。

以下最终 Runtime 结果由用户本次在真实 Spring Boot + MySQL 环境提供，不冒充 Codex 收口轮重跑，不声明服务持续在线：

| 验证 | 真实结果 |
| --- | --- |
| ComparisonMapperIntegrationTests | 4 passed / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS |
| 完整 mvn test | 193 passed / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS |
| 完整 mvn package | 193 passed / 0 failures / 0 errors / 0 skipped，repackage PASS，BUILD SUCCESS |
| Spring Boot Startup | PASS：Tomcat 8080，Started WeatherApplication |

193 = 原有 134 + 新增 59（Service 20、Controller 32、Binding 3、Integration 4）。完整回归覆盖 Comparison、Trend、Workbench、Dictionary、Database Connection。新增测试覆盖非种子 ID、模型字典乱序、各模型排序、缺测、单模型空、全空、真实零、错误状态与参数绑定；集成覆盖 T2M、PRECIP、相等闭区间及 Empty。

## T2M / PRECIP Runtime

济南，2026-09-07 08:00:00～14:00:00；T2M 与 PRECIP 均 HTTP/code 200，两个系列各 3 点，ECMWF/NOAA 顺序正确。

| 时次 | ECMWF T2M（℃） | NOAA T2M（℃） | ECMWF PRECIP（mm） | NOAA PRECIP（mm） |
| --- | --- | --- | --- | --- |
| 08:00 | 19.00 | 18.40 | 0.00 | 0.10 |
| 11:00 | 20.20 | 19.60 | 0.40 | 0.60 |
| 14:00 | 21.10 | 20.50 | 0.20 | 0.30 |

T2M PASS；PRECIP PASS，unit=mm，真实 0.00 正确保留。上述为合成工程演示数据，不是实际 ECMWF/NOAA 预报。

## Error / Boundary / Empty Runtime

- startTime > endTime：HTTP 400，PASS。
- cityId=999999：HTTP 404，PASS。
- elementId=999999：HTTP 404，PASS。
- 2026-09-08 08:00:00～14:00:00：HTTP/code 200，series count=2，ECMWF values=[]、NOAA values=[]，PASS。

## 数据库与前端回归

Database：shandong_weather。Comparison Integration 只读 SELECT，不执行 INSERT/UPDATE/DELETE；每项前后检查 city=16、forecast_model=2、weather_element=2、forecast_record=192。用户确认集成测试通过、事务回滚、数量保持。Schema/Seed unchanged，无新增索引或表。

本阶段 frontend 未修改。实现轮前端 npm test 45 passed / 0 failed，npm run build PASS；收口轮未重跑。Java 17.0.11、Spring Boot 2.7.18、Maven 3.9.16、UTF-8 基线不变，字节码 major version 61。

## Known Limitations

- Seed 为 192 条、3 个固定合成时次，不代表完整 7 天或实时数据。
- 不包含 FE-COMPARISON、CRUD、Cache、Redis、RMSE/MAE/Bias/Accuracy 或模型优劣结论。
- 前端大 chunk 提示为既有非阻塞项。
- 课程设计/.DS_Store 为已跟踪生成文件，独立非阻塞清理项，本阶段不移除。

## 安全与 Git

凭据仅使用本机环境变量，不记录密码，不创建 .env/.mylogin.cnf。收口仅同步两份上下文并创建本档案，不改业务代码或历史归档。

- Branch：feat/be-comparison。
- 开始基线 HEAD 与本地 origin/main：f15832089ec3f2a8431769bbc88567443e9227fd；这是起始基线，不是完成提交。
- 完成 commit/tag/push：未由 Codex 创建或执行，不虚构编号。
- Git writes：NONE；用户通过 GitHub Desktop 检查并操作。

## 下一阶段

BE-COMPARISON-1 COMPLETED；READY FOR FE-COMPARISON-1。

建议 FE-COMPARISON-1，须用户另行授权；本轮不自动开始。
