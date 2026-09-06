<<<<<<< HEAD
# SQL 设计说明 V2
=======
# SQL 设计说明
>>>>>>> acfdd7f44aa976e84028721eacaa4c5e581bdaf7

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
<<<<<<< HEAD
    KEY idx_forecast_record_workbench (model_id, element_id, forecast_time),
=======
    KEY idx_forecast_record_model_id (model_id),
>>>>>>> acfdd7f44aa976e84028721eacaa4c5e581bdaf7
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

<<<<<<< HEAD
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

每城/模型/要素 3 小时一个时次，8×7=56 点；16×2×2×56=3584 条仅估算。示意 2026-09-06 08:00:00～2026-09-13 05:00:00（闭区间，56 点）；正式初始化应选择接近演示日期的窗口并同步前端默认值。济南/青岛用于核心演示；准备无结果、缺测、重复记录和维护刷新用例。

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
=======
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
>>>>>>> acfdd7f44aa976e84028721eacaa4c5e581bdaf7
