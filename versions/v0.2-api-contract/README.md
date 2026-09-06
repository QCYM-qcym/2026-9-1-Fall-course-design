# v0.2 API Contract

## 版本目标

冻结山东省气象预报数据可视化系统前后端 REST API V1.0，为前端 Mock、后端并行开发和后续 Axios 联调提供唯一契约。

## 核心接口

1. `GET /api/cities`
2. `GET /api/forecast-models`
3. `GET /api/weather-elements`
4. `GET /api/forecast-records`
5. `POST /api/forecast-records`
6. `PUT /api/forecast-records/{id}`
7. `DELETE /api/forecast-records/{id}`

## 核心约定

- Base URL：`/api`；
- 时间格式：`yyyy-MM-dd HH:mm:ss`；
- 课程设计数据按统一本地业务时间处理；
- 成功统一使用 HTTP 200 和 `{ code, message, data }`；
- 空查询返回 HTTP 200、`data: []`；
- 错误区分 400、404、409、500；
- 预报查询的城市、模型、要素、开始时间、结束时间均必填；
- 查询结果默认按 `forecastTime` 升序返回；
- V1.0 不分页，不增加独立 chart/statistics API；
- T2M 使用折线图、单位 ℃；PRECIP 使用柱状图、单位 mm；
- 表格和图表使用同一轮 `GET /api/forecast-records` 响应；
- `forecast_record` 业务唯一键冲突返回 409；
- 城市、模型、要素完整 CRUD 与 7 个核心接口存在范围冲突，暂按方案 A 待确认。

## 当前没有实现

- `backend/` 尚未实现；
- `frontend/` 尚未实现；
- `database/` 尚未初始化；
- 尚未执行真实 API 联调或自动化测试；
- 尚未创建独立 Mock 服务。

## 相关文档

- [接口设计 V1.0](../../课程设计/02-系统设计/04-接口设计.md)
- [需求冻结确认](../../课程设计/01-立项与需求/09-需求冻结确认.md)
- [当前项目上下文](../../课程设计/docs/context/CURRENT_CONTEXT.md)

## Git

Branch：`main`

Commit：未提交

Tag：未创建

## 下一版本

`v0.3-project-init`：在当前契约不变的前提下初始化 `frontend/`、`backend/` 和 `database/`，并记录实际环境版本。

