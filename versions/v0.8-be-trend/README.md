# v0.8 — BE-TREND-1

> 收口日期：2026-09-08。状态：BE-TREND-1 COMPLETED。
> Gate：READY FOR FE-TREND-1。

## 任务目标与范围

在冻结 API V2 下完成单城市、单模型的温度/降水趋势查询及四项后端统计，为下一阶段前端接入提供真实 API。仅实现 GET /api/weather/trend，不实现 Comparison、CRUD 或 FE-TREND；Schema、Seed、Workbench 业务逻辑/SQL、Dictionary 行为和前端源码不变。

本目录只保存阶段元数据和验收说明，不复制源码。正式契约见 [API V2](../../课程设计/02-系统设计/04-接口设计.md)，当前状态见 [CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)。

## 接口与结构

- Endpoint：GET /api/weather/trend。
- Params：cityId、modelId、startTime、endTime，全部必填。
- 时间：yyyy-MM-dd HH:mm:ss，Asia/Shanghai 本地业务时间，闭区间；起止相等合法。
- ApiResponse 的 data：cityId、cityName、modelId、modelName、temperature、precipitation、statistics；无额外 city/model 嵌套。
- 两个系列的点：forecastTime、value，各自独立时间升序。
- Statistics：temperatureMax、temperatureMin、temperatureAvg、precipitationTotal。
- 400：缺失/非法参数、时间倒置、存在但不支持的城市或模型；404：城市或模型不存在；500：核心要素缺失或单位配置异常。

调用链：WeatherQueryController → WeatherQueryService → ForecastRecordMapper → ForecastRecordMapper.xml → MySQL；TrendRecordRow 承载查询行，TrendVO 承载正式响应。

## SQL 与要素解析

复用 CityMapper、ForecastModelMapper 获取真实元数据并验证存在性及支持范围。WeatherElementMapper 按 element_code 查询 T2M/PRECIP，解析实际 ID，不硬编码 elementId，也不依赖字典返回顺序。

selectTrendData 查询 forecast_record fr JOIN weather_element we ON fr.element_id = we.id，读取 element_code、forecast_time、value。WHERE 限定 cityId、modelId、实际 elementIds 集合及 forecast_time BETWEEN startTime AND endTime；ORDER BY we.element_code ASC、fr.forecast_time ASC。使用 #{} 参数绑定及 foreach，不使用字符串拼接参数。城市/模型名称复用存在性查询得到的 Entity，无需在主查询追加 JOIN。

## 缺测、Empty 与统计

- 缺测不补 0，不补 null 占位；两系列时次不一致时独立保留真实点，不强制对齐。
- 合法城市/模型但无记录：HTTP/code 200，保留元数据，temperature=[]、precipitation=[]，statistics 四字段均为 null。
- 最大、最小、平均值仅取真实温度样本；平均值 BigDecimal sum/count，HALF_UP 保留两位。
- 降水总量为实际独立时段降水样本之和；无降水样本返回 null，真实零降水保持 0。
- 全部数值使用 BigDecimal，不经 double 计算；不新增统计表、索引、缓存或预计算。

## 测试与证据来源

实现轮 Codex 执行 TDD：先验证失败再实现；Service 20、Controller 31、Binding 3，连同既有 67 项非数据库测试，共 121 passed / 0 failures / 0 errors / 0 skipped。test-compile、限定这些测试的 package/repackage 通过；独立审查无 Critical/Important，补充了双系列不同时次缺测测试。

最终 Runtime 依据用户本次提供的真实本机 PowerShell 与 HTTP 验收结果，非 Codex 收口轮重新运行，也不声明服务持续在线：

| 验证 | 结果 |
| --- | --- |
| TrendMapperIntegrationTests | 4 passed / 0 failures / 0 errors / 0 skipped；BUILD SUCCESS |
| 完整 mvn test | 134 passed / 0 failures / 0 errors / 0 skipped；BUILD SUCCESS |
| mvn package | 134 passed / 0 failures / 0 errors / 0 skipped；repackage PASS；BUILD SUCCESS |

完整 134 项包含原有 76 项与新增 Trend 58 项（Service 20、Controller 31、Binding 3、Integration 4）。真实集成覆盖济南 ECMWF、NOAA、相等边界/真实零降水及 Empty。

## 真实 HTTP 验收

济南 + ECMWF，2026-09-07 08:00:00～14:00:00：HTTP/code 200，cityId=1、modelId=1、modelName=ECMWF，temperature 与 precipitation 各 3 点。

| 时间 | T2M（℃） | PRECIP（mm） |
| --- | --- | --- |
| 08:00 | 19.00 | 0.00 |
| 11:00 | 20.20 | 0.40 |
| 14:00 | 21.10 | 0.20 |

统计：temperatureMax=21.10、temperatureMin=19.00、temperatureAvg=20.10、precipitationTotal=0.60。

- Normal HTTP：PASS。
- startTime=14:00、endTime=08:00：HTTP 400，PASS。
- cityId=999999：HTTP 404，PASS。
- 2026-09-08 08:00:00～14:00:00：HTTP/code 200、两个空数组、四项统计 null，Empty PASS。
- 用户既有 API 回归：health code=200/data=ok；字典数量 16/2/2；Workbench ECMWF/T2M code=200、3 times/48 records。

## 数据库与前端状态

Database：shandong_weather。只读核对 TrendMapperIntegrationTests 源码，确认每项前后均断言 city=16、forecast_model=2、weather_element=2、forecast_record=192，且验证 SELECT DATABASE()。用户真实 4 项测试通过作为本阶段四表完整性证据。测试仅 SELECT，不执行 INSERT/UPDATE/DELETE；Schema、Seed unchanged。

前端源码未修改；实现轮 npm test 20 passed、npm run build PASS，最终文档收口未重跑。FE-TREND-1 尚未开始，不作为本阶段后端缺陷。

## Known Limitations

- 当前只有 3 个固定合成演示时次，不代表完整 7 天或实时气象数据。
- PowerShell 中 cityName 中文显示乱码按用户确认属于终端编码；此前浏览器与接口数据已确认济南映射正确，本轮不修改业务代码或全局编码环境。
- 前端大 chunk 提示为既有非阻塞 Minor。
- 既有课程设计/.DS_Store 仍被跟踪，是独立非阻塞仓库清理项，未自行移除。

## 安全与 Git

凭据仅由本机环境变量提供；不记录密码，不创建 .env 或 .mylogin.cnf。Java 17.0.11、Spring Boot 2.7.18、Maven 3.9.16、UTF-8 基线不变。

- Branch：feat/be-trend。
- 开始基线 HEAD 与本地 origin/main：274773c9d1e3b491db8b8f0c907d66ccdbfe1c62；这是起始基线，不是本阶段完成提交。
- 本阶段完成 commit/tag/push：未由 Codex 创建或执行，不虚构编号。
- Git writes：NONE；暂存、提交和推送由用户通过 GitHub Desktop 操作。

## 下一阶段

建议 FE-TREND-1。BE-TREND-1 COMPLETED；READY FOR FE-TREND-1。须用户另行授权，不自动开始。
