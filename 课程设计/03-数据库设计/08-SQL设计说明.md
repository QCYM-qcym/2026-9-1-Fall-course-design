# SQL 设计说明 V2

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
    KEY idx_forecast_record_workbench (model_id, element_id, forecast_time),
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

forecast_record.value 不增加跨要素统一 CHECK；Service 校验有限、DECIMAL(10,2) 精度、PRECIP ≥ 0，T2M 无人为气象范围。本页仅文档，未执行建库建表。

## 4. 最小索引确认

预报记录二级索引恰为三项：业务唯一索引、workbench 联合索引、element_id 单列索引，另有 PRIMARY(id)。新的 workbench 索引以 model_id 开头，因此替换旧 model_id 单列；业务唯一索引覆盖 city_id 外键左前缀；element_id 单列仍需保留。暂不另建趋势、对比或纯时间索引，详见 [索引取舍](07-索引设计.md)。

## 5. 初始化数据范围（未生成）

顺序：城市 → 模型 → 要素 → 预报记录。城市编码沿用既有风格，补齐全部山东 16 地级市；坐标未来按来源核验，不在此伪造完整坐标集。

| city_code | city_name |
| --- | --- |
| JINAN | 济南 |
| QINGDAO | 青岛 |
| ZIBO | 淄博 |
| ZAOZHUANG | 枣庄 |
| DONGYING | 东营 |
| YANTAI | 烟台 |
| WEIFANG | 潍坊 |
| JINING | 济宁 |
| TAIAN | 泰安 |
| WEIHAI | 威海 |
| RIZHAO | 日照 |
| LINYI | 临沂 |
| DEZHOU | 德州 |
| LIAOCHENG | 聊城 |
| BINZHOU | 滨州 |
| HEZE | 菏泽 |

数据库存短名；未来 GeoJSON properties.name 全名由前端 cityCode 静态映射适配。地图始终显示 16 区，新增临时城市不进入核心地图。模型 ECMWF/NOAA，要素 T2M（2 米气温，℃）、PRECIP（降水量，mm），id 在未来初始化时获取，不假设固定值。

当前MONTHLY-SYNTHETIC-DATA-1采用2026-09-01～30，每日02/05/08/11/14/17/20/23；每城/模型/要素240点，16×2×6×240=46,080条，生成与隔离库实数均已核验。原七天3584条只是旧估算，不再用于新初始化。生成器固定seed20260901，开发期产生database/data.sql，目标电脑不运行生成器。默认演示仍9/7 08～14，济南十二个温度/降水验收值保留；无数据窗口应选10月等范围。详见[月级数据说明](../docs/MONTHLY-SYNTHETIC-DATA-1.md)。

PRECIP 为截至有效时刻的前 3 小时非重叠时段量。若来源是起报累计降水，未来离线整理须先转换；保留来源/许可、单位、时间和转换说明。合成数据须明示，不得包装成真实模型预测。不得本轮生成正式 forecast 数据或创建 database/。

## 6. 三个核心 XML 查询设计

下述是文档中的参数化 SQL 形态，不是已创建的 Mapper XML。正式 XML 使用 #{参数} 绑定和 foreach 绑定列表，禁止用户输入字符串拼接。cityCodes 为上表固定 16 编码；核心 elementIds/modelIds 由 Service 按编码解析字典，不能硬编码自增 id。SQL 采用 :name 作为阅读占位符，不能原样当 XML 执行。

### selectWorkbenchData

```sql
SELECT r.city_id, c.city_name, r.forecast_time, r.value,
       m.id AS model_id, m.model_code, m.model_name,
       e.id AS element_id, e.element_code, e.element_name, e.unit
FROM forecast_record r
JOIN city c ON c.id = r.city_id
JOIN forecast_model m ON m.id = r.model_id
JOIN weather_element e ON e.id = r.element_id
WHERE r.model_id = :modelId
  AND r.element_id = :elementId
  AND r.forecast_time BETWEEN :startTime AND :endTime
  AND c.city_code IN (:cityCodes)
ORDER BY r.forecast_time, r.city_id;
```

Service 从实际记录取 times 去重升序并组装 WorkbenchVO，元数据即使空结果也从字典校验结果保留。一次批量最多计划约 896 条，时间轴之后不查库。

### selectTrendData

```sql
SELECT r.forecast_time, r.value, e.element_code, e.unit,
       c.id AS city_id, c.city_name, m.id AS model_id, m.model_name
FROM forecast_record r
JOIN city c ON c.id = r.city_id
JOIN forecast_model m ON m.id = r.model_id
JOIN weather_element e ON e.id = r.element_id
WHERE r.city_id = :cityId
  AND r.model_id = :modelId
  AND r.element_id IN (:temperatureId, :precipitationId)
  AND r.forecast_time BETWEEN :startTime AND :endTime
ORDER BY e.element_code, r.forecast_time;
```

Service 按 T2M/PRECIP 分组、保留序列升序、计算四项统计；空统计 null，不新增表。业务唯一索引前缀可用于城市/模型定位，不保证任意条件下都无排序。

### selectComparisonData

```sql
SELECT r.forecast_time, r.value,
       c.id AS city_id, c.city_name,
       m.id AS model_id, m.model_code, m.model_name,
       e.id AS element_id, e.element_code, e.element_name, e.unit
FROM forecast_record r
JOIN city c ON c.id = r.city_id
JOIN forecast_model m ON m.id = r.model_id
JOIN weather_element e ON e.id = r.element_id
WHERE r.city_id = :cityId
  AND r.element_id = :elementId
  AND r.model_id IN (:ecmwfId, :noaaId)
  AND r.forecast_time BETWEEN :startTime AND :endTime
ORDER BY m.model_code, r.forecast_time;
```

Service 明确按 ECMWF、NOAA 顺序组装两个系列，包括空系列；前端以两组时间并集对齐，无值 null。管理 GET 复用四表 JOIN，按传入条件动态 WHERE，时间成对；按 forecast_time、city_id、model_id、element_id、id 稳定排序，可使用同 XML 的 selectManagementRecords，不新增 Mapper。

## 7. 后续实施前检查（均未运行）

- 确认 MySQL ≥8.0.16、InnoDB、utf8mb4、严格 SQL 模式和本地业务时间口径；凭据不入库。
- 重新确认目标为授权开发环境及备份，不能对未知已有数据库运行 DDL。
- 核对四表/字段/约束与数据字典一致，实际 SHOW CREATE TABLE、SHOW INDEX 检查外键、CHECK、唯一键和三项二级索引。
- 三个查询执行 EXPLAIN，记录真实 key/rows/Extra；不要把索引设计写成已验证性能。
- 校验 16 市映射、两模型两要素、56 时次与计划总数；id 满足 API 安全整数范围。
- 负降水、非法精度/时间、不存在关联、重复键、引用删除覆盖错误；统计与缺测按 API 契约验证。
- 本轮没有 MySQL 执行、初始化脚本或正式数据。

## 相关资料

- [数据需求](../01-立项与需求/08-数据需求.md)
- [逻辑结构](03-逻辑结构设计.md)
- [数据字典](05-数据字典.md)
- [完整性约束](06-完整性约束设计.md)
- [索引设计](07-索引设计.md)
- [API V2](../02-系统设计/04-接口设计.md)
- [项目导航](../00-项目总览/项目导航.md)
