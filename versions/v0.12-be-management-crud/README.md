# v0.12 BE Management CRUD

## Goal

BE-MANAGEMENT-CRUD-1 COMPLETED。完成四类资源基础 CRUD，Management API：16 / 16 IMPLEMENTED。本归档仅记录阶段事实，不复制源码。

## API

| 资源 | 路径 | 操作 | 状态 |
|---|---|---|---|
| City | /api/cities | GET / POST / PUT / DELETE | 4 / 4 |
| Forecast Model | /api/forecast-models | GET / POST / PUT / DELETE | 4 / 4 |
| Weather Element | /api/weather-elements | GET / POST / PUT / DELETE | 4 / 4 |
| Forecast Record | /api/forecast-records | GET / POST / PUT / DELETE | 4 / 4 |

PUT / DELETE 使用 /{id}。GET /api/forecast-records 全量返回、id ASC，无分页或高级筛选；扩展响应字段保持冻结契约：id、cityId/cityCode/cityName、modelId/modelCode/modelName、elementId/elementCode/elementName/unit、forecastTime、value。

## Architecture

Controller → Service → Mapper → MySQL。扩展既有三个字典 Controller / Service，使用专用写 Request DTO，不直接接收 Entity。

新增 ForecastRecord Entity / Service / Controller，既有 ForecastRecordMapper 扩展 BaseMapper，没有第二套 Mapper；Workbench / Trend / Comparison 自定义查询保持兼容。

写操作使用事务；协调字典行锁保护引用语义。记录全量读取采用只读一致性事务。Management 集成测试使用事务回滚，不扩展复杂事务架构。

## Validation

- 必填、字符串长度、经纬度范围、数值精度、严格 LocalDateTime、安全 ID、外键关联存在检查。
- PRECIP 不允许负数；真实 0.00 合法，不补缺测零值。
- 沿用 ApiResponse、BusinessException、GlobalExceptionHandler：Validation / JSON 解析错误 400，资源不存在 404，唯一/引用冲突 409，未知异常安全返回 500。

## Conflict Semantics

cityCode、modelCode、elementCode 重复 → 409；forecast_record 的 (cityId, modelId, elementId, forecastTime) 重复 → 409。Service 主动判重，数据库 UNIQUE / FK 作为兜底。

## Reference Protection

被 forecast_record 引用的 City / Forecast Model / Weather Element 删除 → 409；不存在 → 404。无 Cascade Delete、无 Logical Delete；ForecastRecord 本身允许直接删除。

已引用时，cityCode、modelCode、elementCode / unit 不允许改变，冲突返回 409；展示性字段仍可修改。模型 description 可清空为 null。

## Tests

实现阶段证据：City CRUD 24 PASS、Forecast Model CRUD 20 PASS、Weather Element CRUD 22 PASS、Forecast Record CRUD 28 PASS、Management Mapper Binding 3 PASS。

全部非数据库测试：273 PASS / 0 failures / 0 errors / 0 skipped。

依据用户提供的本机真实 MySQL Runtime 验收结果：

- ManagementCrudIntegrationTests：PASS。
- Full mvn test：PASS。
- 未提供完整 Maven Tests run 数量，不推算总数。

本次收口未重跑 Maven 或 HTTP，不将用户验收写为 Codex 本轮执行。

## Package

Full mvn package：PASS；Spring Boot repackage：PASS。依据用户本机真实执行结果。

## Runtime

BE-MANAGEMENT-CRUD-1 Runtime Validation：PASS，依据用户提供的本机真实 MySQL Runtime 验收结果。

- GET Cities / Models / Elements / Records：PASS。
- City POST / PUT / DELETE：PASS；duplicate cityCode 409、referenced DELETE 409：PASS。
- Forecast Model POST / PUT / DELETE、referenced DELETE 409：PASS。
- Weather Element POST / PUT / DELETE、referenced DELETE 409：PASS。
- Forecast Record POST / PUT / DELETE、duplicate combination 409：PASS。
- Missing City DELETE 404、PRECIP negative 400、PRECIP zero 200：PASS。
- Runtime 使用临时数据，用户确认已完成清理；不声明服务当前持续在线。

## Database Baseline

顺序：city / forecast_model / weather_element / forecast_record。

Initial：16 / 2 / 2 / 192，PASS。

Final：16 / 2 / 2 / 192，PASS。

数量与清理结果依据用户真实验收；集成测试使用事务回滚。

## Regression

用户真实 Runtime：Dictionary（Cities / Models / Elements）、Workbench、Trend、Comparison 全部 PASS。本阶段验收未发现破坏三大核心查询。

## Schema / Seed

Schema：UNCHANGED。Seed：UNCHANGED。Index：UNCHANGED。仍为四张核心表，没有增加字段、表或索引。

本阶段未实现 FE Management，/management 仍为 PLACEHOLDER；/weather、/analysis、/comparison 保持 COMPLETED。

## Known Non-blocking Items

- 既有已跟踪的课程设计/.DS_Store 为独立仓库清理项，本轮不移除。
- 无分页、高级筛选、批量 CRUD、导入导出、缓存或权限系统，保持本阶段范围。
- 本次收口只更新两份上下文及本归档，不修改业务、前端、数据库脚本或连接配置。
- 收口分支：feat/be-management-crud。Git writes = NONE；未创建或虚构 commit / tag / push，提交由用户通过 GitHub Desktop 操作。

## Next Gate

READY FOR FE-MANAGEMENT-CRUD-1。下一阶段须另行授权，本轮不自动开始。
