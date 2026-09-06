# v0.4 Database Init

## 版本目标

完成四张核心表的可执行 MySQL DDL、基础字典数据和少量可重复的工程演示数据，为后续 Workbench 查询开发提供稳定数据库基线。

## 版本状态

- Branch: `feat/db-init`
- Commit: 未提交（由用户使用 GitHub Desktop 完成）
- Tag: 未创建
- 当前结论：`DB-INIT-1 COMPLETED`

## 数据库实现

- 数据库 `shandong_weather`，MySQL 8.0.16+、InnoDB、utf8mb4。
- 仅四张表：`city`、`forecast_model`、`weather_element`、`forecast_record`。
- 主键、三组字典代码唯一约束、预报记录业务唯一键、三项冻结索引和三组 RESTRICT 外键已写入 `schema.sql`。
- 初始化数据为 16 个山东地级市、ECMWF/NOAA、T2M/PRECIP，以及 2026-09-07 三个时间点共 192 条固定记录。
- 城市名称遵循数据字典冻结的短名称。坐标来自 GeoNames 城市/行政中心结果并四舍五入到 6 位小数；预报记录为合成工程演示数据，不是真实气象预报。

## 验证记录

静态验证：

- PASS：脚本包含四张表、固定字典集合、192 条显式记录、无随机数和无额外业务表。
- PASS：DDL 依赖顺序、唯一键、索引、外键名称及字符集配置已按设计文档核对。
- PASS：前端 `npm run build`、后端 `mvn test` 和 `mvn package` 本轮复跑通过；后端测试为 1/0/0/0。本轮未修改前后端。

MySQL 运行验证：

- Runtime：PASS；MySQL Server 8.0.46，服务 `MySQL80`，localhost:3306。
- DDL / Seed：PASS；四张核心表均创建成功，行数为 16 / 2 / 2 / 192，有效时间 3 个。
- Batch：PASS；12 个模型/要素/时间组合均为 16 市。
- Unique：PASS；重复业务键被 `uq_forecast_record_business` 拒绝，错误 1062。
- FK：PASS；非法 `city_id` 被 `fk_forecast_record_city` 拒绝，错误 1452；三组外键均真实存在。
- RESTRICT：PASS；删除已引用城市被拒绝，错误 1451；数据仍为 16 市、192 条记录。
- JOIN：PASS；济南 + ECMWF + T2M 返回 3 条并按时间升序。
- SHOW INDEX：PASS；索引及列顺序与冻结设计一致。
- EXPLAIN：PASS；`possible_keys` 包含 `idx_forecast_record_workbench` 和 `idx_forecast_record_element_id`，实际 `key=idx_forecast_record_element_id`、`type=ref`、`rows=96`、`Using where; Using filesort`。192 条小数据下优化器选择成本更低的访问路径，不调整冻结索引。
- Charset：PASS；数据库和四表为 utf8mb4 / utf8mb4_unicode_ci，中文与 `℃` 正常。
- Sample：PASS；PRECIP 无负值，模型有差异，时间值有变化，坐标完整且范围合理。

## 当前边界

本版本不包含 Java 业务代码、Vue 业务页面、API 实现、数据库在线采集或真实 ECMWF/NOAA 数据。DB-INIT-1 已完成运行态验收，应用数据库账号与 Spring Boot 数据库连接留待后续任务处理。

## 下一步

建议下一阶段为 `BE-WORKBENCH-1`，但本版本不自动开始后端业务开发。
