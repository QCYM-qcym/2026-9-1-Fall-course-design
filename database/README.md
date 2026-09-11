# 数据库初始化

## 当前月级合成数据

MONTHLY-SYNTHETIC-DATA-1 新数据由 `node tools/data/generate-monthly-weather-data.mjs` 在仓库根目录生成 `database/data.sql`。固定seed=20260901，保留原16市字典，模型场景ECMWF/NOAA，六要素，2026-09-01～30每日02/05/08/11/14/17/20/23。理论46,080条/240时次，真实生成与隔离MySQL验收记录见 [月级数据说明](../课程设计/docs/MONTHLY-SYNTHETIC-DATA-1.md)。

这是课程合成天气过程，不是真实ECMWF/NOAA数据、API实时采集或历史实测天气。六要素并非独立随机：云雨湿和昼夜温差相关，风由内部u/v推导，RH由T2M/内部露点近似推导。原JINAN 9/7三时次温度和降水十二值明确override保留。

新电脑/portable仅携带生成后的SQL，无需安装Node或运行时随机生成。原schema.sql不改，仅对新的空隔离实例依次导入schema/data。schema会DROP四张表，严禁直接在唯一有效开发库或旧演示目录中执行。旧v1.0目录不自动升级；以下192条、两要素是历史初始化与验收记录，不是当前新SQL的范围。

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
- `weather_element`：`T2M`（℃）、`PRECIP`（mm）、`TCC`（%）、`WIND_SPEED_100M`（m/s）、`WIND_DIR_100M`（°）、`RH`（%）。
- `forecast_record`：2026-09-01～30，每日02/05/08/11/14/17/20/23；真实生成与隔离库查询均为46,080条、240时次。
- `forecast_record` 的业务唯一键为 `(city_id, model_id, element_id, forecast_time)`；保留设计冻结的三个二级索引。

城市名称按数据字典冻结的短名称保存，例如“济南”，不改写为“济南市”。城市经纬度取自 [GeoNames Shandong coordinate results](https://www.geonames.org/advanced-search.html?q=Shandong,+China)，四舍五入到 6 位小数；这些坐标用于工程地图映射与约束验证，不宣称测绘级精度。

`data.sql` 当前46,080条记录来自固定seed的共享合成天气过程，用于查询、约束及展示联调；不代表真实 ECMWF/NOAA 数据，没有在线采集。下节2026-09-06运行结果保留旧192条历史事实。

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
