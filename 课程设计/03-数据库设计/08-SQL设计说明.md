# SQL 设计说明

## 1. 设计状态与目标版本

本文是待执行的 DDL 与初始化设计稿，不表示数据库、数据表、约束或索引已经创建。目标实例固定为 MySQL 8.0.16+，以确保经纬度 `CHECK` 约束能够由数据库实际执行。

## 2. 建表与删除顺序

建表顺序固定为：

1. `city`
2. `forecast_model`
3. `weather_element`
4. `forecast_record`

删除顺序必须反向执行：

1. `forecast_record`
2. `weather_element`
3. `forecast_model`
4. `city`

这样可避免外键依赖导致建表或删除失败。实际执行删除前必须确认目标环境及数据备份，不在本阶段执行任何删除语句。

## 3. 待执行 DDL

```sql
CREATE TABLE city (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    city_code VARCHAR(32) NOT NULL,
    city_name VARCHAR(50) NOT NULL,
    longitude DECIMAL(10,6) NOT NULL,
    latitude DECIMAL(10,6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_city_city_code UNIQUE (city_code),
    CONSTRAINT chk_city_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_city_latitude CHECK (latitude BETWEEN -90 AND 90)
);

CREATE TABLE forecast_model (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    model_code VARCHAR(32) NOT NULL,
    model_name VARCHAR(50) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_forecast_model_model_code UNIQUE (model_code)
);

CREATE TABLE weather_element (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    element_code VARCHAR(32) NOT NULL,
    element_name VARCHAR(50) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_weather_element_element_code UNIQUE (element_code)
);

CREATE TABLE forecast_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    city_id BIGINT UNSIGNED NOT NULL,
    model_id BIGINT UNSIGNED NOT NULL,
    element_id BIGINT UNSIGNED NOT NULL,
    forecast_time DATETIME NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_forecast_record_business
        UNIQUE (city_id, model_id, element_id, forecast_time),
    KEY idx_forecast_record_model_id (model_id),
    KEY idx_forecast_record_element_id (element_id),
    CONSTRAINT fk_forecast_record_city
        FOREIGN KEY (city_id) REFERENCES city (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_forecast_record_model
        FOREIGN KEY (model_id) REFERENCES forecast_model (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_forecast_record_element
        FOREIGN KEY (element_id) REFERENCES weather_element (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);
```

`forecast_record.value` 不建立统一数据库 `CHECK`。T2M 与 PRECIP 采用不同的系统输入校验范围，由业务层读取 `weather_element.element_code` 后分别判断。

## 4. 索引确认

- 四张表的主键形成主键索引。
- `city_code`、`model_code`、`element_code` 的唯一约束形成唯一索引。
- `uq_forecast_record_business` 同时承担业务判重和核心组合查询职责。
- 组合唯一索引以 `city_id` 为最左列，因此不重复创建单列 `city_id` 索引。
- 为 `model_id`、`element_id` 分别保留单列索引，支持外键检查与关联访问。
- 暂不为 `forecast_time`、名称、说明、单位、坐标和 `value` 单独建立索引。

## 5. 初始化数据顺序与要求

初始化顺序固定为：城市 → 预报模型 → 气象要素 → 预报记录。

- 城市包含以下编码稳定且名称唯一的山东演示城市，并提供与城市匹配的合法经纬度：

| `city_code` | `city_name` |
| --- | --- |
| `JINAN` | 济南 |
| `QINGDAO` | 青岛 |
| `YANTAI` | 烟台 |
| `WEIFANG` | 潍坊 |
| `LINYI` | 临沂 |
| `JINING` | 济宁 |

- 预报模型至少包含 ECMWF、NOAA。
- 气象要素包含 T2M（℃）、PRECIP（mm）。
- 预报记录覆盖济南核心查询、青岛城市切换、约 7 天数据、T2M 折线图、PRECIP 柱状图、无结果查询和管理校验场景。
- 同一 `(city_id, model_id, element_id, forecast_time)` 只允许一条记录。
- T2M 系统输入校验范围为 `-80 ≤ value ≤ 60`，PRECIP 为 `0 ≤ value ≤ 1000`；这些是课程系统输入范围，不是气象学绝对极限。

本阶段只确认初始化口径，不编写或执行正式初始化脚本。

## 6. DDL 实施前检查

- 确认 MySQL 版本不低于 8.0.16。
- 确认目标库字符集、时区和连接信息，且执行环境不是已有业务数据库。
- 再次核对四张表、字段类型、约束名和索引名与数据字典一致。
- 确认经纬度 `CHECK` 在目标实例中实际生效。
- 确认业务层将实现 T2M、PRECIP 的差异化输入校验。
- 确认济南、青岛、烟台、潍坊、临沂、济宁、ECMWF、NOAA、T2M、PRECIP 的演示数据准备方案。
- 建表后应检查 `SHOW CREATE TABLE` 和索引清单，再进行初始化；这些操作均留待工程阶段执行。

## 7. 相关资料

- [[01-立项与需求/09-需求冻结确认|需求冻结确认]]
- [[03-数据库设计/03-逻辑结构设计|逻辑结构设计]]
- [[03-数据库设计/05-数据字典|数据字典]]
- [[03-数据库设计/06-完整性约束设计|完整性约束设计]]
- [[03-数据库设计/07-索引设计|索引设计]]
- [[00-项目总览/项目导航|项目导航]]
