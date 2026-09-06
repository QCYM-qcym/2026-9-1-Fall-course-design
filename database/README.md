# 数据库初始化

本目录保存当前可执行的 MySQL 初始化脚本，服务于冻结的 Database V2 四表设计。

## 运行目标

- 数据库：`shandong_weather`
- 版本：MySQL 8.0.16+
- 存储引擎：InnoDB
- 字符集：`utf8mb4`，排序规则：`utf8mb4_unicode_ci`
- 表：`city`、`forecast_model`、`weather_element`、`forecast_record`

执行顺序：

1. 先执行 `schema.sql`，创建数据库并重建四张表。
2. 再执行 `data.sql`，写入字典数据和演示记录。

`schema.sql` 可重复执行，但会按依赖逆序删除并重建上述四张表；不要在包含重要业务数据的数据库上直接执行。脚本不会删除整个数据库，也不会创建四表之外的业务表。

## 初始化内容

- `city`：山东 16 个地级市，城市代码稳定且唯一。
- `forecast_model`：`ECMWF`、`NOAA`。
- `weather_element`：`T2M`（℃）、`PRECIP`（mm）。
- `forecast_record`：2026-09-07 08:00、11:00、14:00 三个时间点，共 16 × 2 × 2 × 3 = 192 条记录。
- `forecast_record` 的业务唯一键为 `(city_id, model_id, element_id, forecast_time)`；保留设计冻结的三个二级索引。

城市名称按数据字典冻结的短名称保存，例如“济南”，不改写为“济南市”。城市经纬度取自 [GeoNames Shandong coordinate results](https://www.geonames.org/advanced-search.html?q=Shandong,+China)，四舍五入到 6 位小数；这些坐标用于工程地图映射与约束验证，不宣称测绘级精度。

`data.sql` 中的 192 条记录是固定的合成工程演示数据，只用于验证查询、唯一键、外键、索引和前后端联调，不代表真实 ECMWF/NOAA 数据，也没有使用随机数或在线采集。

## MySQL 8.0.46 运行验证

2026-09-06 已在本机 MySQL 8.0.46 上依次执行 `schema.sql` 和 `data.sql`，结果如下：

- DDL：PASS；只创建 `city`、`forecast_model`、`weather_element`、`forecast_record` 四张业务表。
- Seed：PASS；四表行数分别为 16、2、2、192，预报有效时间为 3 个。
- Workbench 批次：PASS；2 × 2 × 3 共 12 个模型/要素/时间组合，每组均覆盖 16 市。
- Unique：PASS；重复业务键被 `uq_forecast_record_business` 拒绝，记录数保持 192。
- FK：PASS；不存在的 `city_id` 被 `fk_forecast_record_city` 拒绝，三组外键元数据均存在。
- RESTRICT：PASS；删除已被引用的城市被拒绝，城市与预报记录未受污染。
- JOIN：PASS；济南 + ECMWF + T2M 返回按 `forecast_time` 升序排列的 3 条记录。
- Index：PASS；`SHOW INDEX` 确认 PRIMARY、业务唯一索引、Workbench 联合索引和 `element_id` 索引。小数据集上的 `EXPLAIN` 将 Workbench 索引列为候选，实际选择 `idx_forecast_record_element_id`（`type=ref`，`rows=96`）并使用 filesort，未因此增加冗余索引。
- Charset：PASS；数据库和四表均为 `utf8mb4` / `utf8mb4_unicode_ci`，中文城市名与 `℃` 显示正常。
- 样例质量：PASS；PRECIP 负值为 0 条，ECMWF/NOAA 的 96 组可比记录中有 91 组数值不同，济南 ECMWF/T2M 三个时间点的值均不同，16 市坐标完整且在合理山东范围内。

## 当前运行状态

MySQL Server 8.0.46、`MySQL80` 服务和 localhost:3306 已完成真实验证；登录凭据未写入仓库。结论为：

`DB-INIT-1 COMPLETED`
