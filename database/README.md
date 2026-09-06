# Database initialization skeleton

目标数据库：MySQL 8.0.16+。

本阶段只建立脚本目录和职责说明，不执行 DDL、不自动建库、不导入数据。

后续初始化顺序：

1. `schema.sql`：四张业务表及经过审查的约束、索引。
2. `data.sql`：山东 16 市、ECMWF/NOAA、T2M/PRECIP，以及必要的演示数据。

四张表固定为 `city`、`forecast_model`、`weather_element`、`forecast_record`。DB-INIT-1 完成正式 DDL 和少量可验证数据后，才可记录数据库结构已经创建。
