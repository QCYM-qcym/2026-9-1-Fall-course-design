# MONTHLY-SYNTHETIC-DATA-1 Implementation Plan

> 使用 Superpowers writing-plans 与 TDD；在当前会话实施并独立复核。用户已批准设计，不重复 brainstorming；禁止所有 Git 写操作，保留已有中期报告。

**Goal:** 生成 2026-09-01～30、02/05/08/11/14/17/20/23 的 16 城市×2 模型×6 要素数据，实数核对 46,080 条、240 时次。

**Architecture:** 四表和十九个业务 API 不变；开发期 Node 确定性生成批量 UTF-8 SQL，portable 继续直接导入 data.sql。Workbench/Comparison 扩六要素，Trend 保持 T2M/PRECIP；不主动新增分页或 UI 重构。若真实全量表不可用，按用户第十四条例外做最小本地分页，不改 GET 全量契约。

**Tech Stack:** 现有 Node ES module、Vue、Java 17、Spring Boot 2.7.18、MyBatis-Plus/XML、MySQL 8。

**Spec:** 用户 MONTHLY-SYNTHETIC-DATA-1 附件完整需求（本会话）；本文保存实施步骤，不代替冻结需求。

## 全局限制与开始检查

- 原 Schema 保持不变，禁止新增业务表、权限、实时采集或误差指标。
- 六要素依次 T2M/℃、PRECIP/mm、TCC/%、WIND_SPEED_100M/m/s、WIND_DIR_100M/°、RH/%；u100/v100/d2m 仅为生成器内部量。
- 两模型共享天气过程并加入小偏差；seed 固定，SQL 无运行时间戳、机器路径或凭据。
- 济南 9/7 08/11/14 温度与降水的十二个 v1.0 样例不变，override 明文标注。
- 只对新建隔离数据库目录初始化，不改开发库3306、不覆盖已有演示目录，不重新打包910MB ZIP。
- 已读四表 DDL：通用 element_id/value 可容纳新增要素，NO_SCHEMA_CHANGE_REQUIRED。
- 起始 main；仅中期检查目录为既有未跟踪改动，保留。依据用户当前实施授权留在原工作目录，不执行分支操作。

## Task 1 生成器与应用兼容 TDD

文件：新增 tools/data/generate-monthly-weather-data.mjs 及 .test.mjs，必要字典输入文件；生成 database/data.sql；最小修改 backend/src 及对应测试、frontend/src 的要素限制与单位映射及对应测试。

- [x] 先写生成器行为测试并实际 RED：固定 seed、全部组合与日历、无重复、范围、零雨与连续雨、天气相关性、风公式、RH 公式、模型相关、十二样例。测试直接执行生成器，不用源码字符串充当行为断言。
- [x] 实现共享平滑天气过程与城市差异，T2M/PRECIP/TCC 直接生成，u/v→风、T/D→RH；使用明确水面 Magnus 近似并记录来源。使用独立字典输入保持原城市编码/坐标，不依赖将被覆盖的生成输出作为下一次输入。
- [x] 接口约定：生成函数返回六要素记录和供测试核对的内部变量；CLI 写确定性批量 SQL。两次执行比较实际字节哈希、解析 SQL 数量和业务键，不能只比较公式。
- [x] 审查 T2M/PRECIP/192 硬编码并按业务限制、默认、样例、旧基线分类。先增加六要素 Workbench/Comparison 和前端单位/图层失败用例，再最小修复。Trend 两要素不扩大。
- [x] 更新真正变化的数据库基线断言至16/2/6/46080；空窗口移到月外。原济南值、异常、CRUD和绑定用例保留，不删除测试。
- [x] 分别运行 node --test tools/data/*.test.mjs、frontend npm test/build、后端非数据库测试与 test-compile；完整 package 留隔离库准备后执行。

## Task 2 隔离数据库与 Runtime

文件：必要验收脚本在 tools/data/，生成物仅已有忽略目录 .portable-build/；不读取或复制开发密码。

- [x] 复用官方已核验 MySQL/Java 程序，在新目录初始化独立实例；只回环监听，端口占用停止。生成仅本地私有凭据，不输出或入库源码。
- [x] 导入原 schema.sql 与生成 data.sql，实际查16/2/6/46080、240时次、每组240、重复0、最早/最晚时间。
- [x] 对济南/青岛/烟台、双模型六要素抽查晴雨风过程，记录真实数值与合理性指标，不声称气象准确率。
- [x] 完整 mvn package（不跳过DB测试），针对本轮隔离库；真实三查询与四类GET验收，并记录全量列表响应体大小/耗时。
- [x] 构建合并JAR在隔离实例运行；浏览器 /weather 与 /comparison 六要素、/analysis 旧契约、/management 大列表。记录实际观察，不因HTTP成功就声称页面可用。
- [x] 直接使用 portable 当前初始化脚本及新SQL验证链路；不制作完整发布ZIP。正常停止仅本次归属进程，保留失败现场。

## Task 3 文档、复核与收口

文件：database/README.md、数据设计说明、当前需求/API/字典范围、AGENTS、CURRENT_CONTEXT/RESUME 必要状态；历史 versions/v1.0 和中期报告不改。

- [x] 记录 seed/公式/天气过程/override、真实生成统计、测试与运行来源及失败；旧 v1.0 数量明确历史口径。
- [x] 独立审查实现与测试，修正实质问题后重跑对应检查。
- [x] git diff --check、范围/冲突/凭据/生成物检查。缺真实页面或数据库证据时保留 RUNTIME_VALIDATION_PENDING，不进入下一阶段。

## 执行记录

计划已依据当前四表、portable data.sql 导入链路及用户冻结范围复核。Task 1 产生 SQL，Task 2 消费同一 SQL；前后端共同以六编码/单位为边界，不扩 Trend。已按本轮实际结果完成勾选；最终157项前端、324项后端与10项生成器测试通过。

2026-09-11 Runtime 触发的必要修复：六图层面板下部被时间轴遮挡；46,080条全量表首次点击到截图约34.6秒，DOM约46,096行（含隐藏城市表）、92,196按钮，随后刷新和关闭标签超时。根据用户明确的“真实浏览器明显不可用”例外，预报记录表增加100条本地分页；保留全部GET数据、ID升序、写后刷新，不增加后端分页/筛选。两项均先加测试再修复，不属整体美化。

最终收口：324项完整package/repackage、157项默认前端测试、10项生成器测试通过；四页实际兼容通过，最终新库16/2/6/46,080、240时次、重复0。原Schema与历史版本无差异。新隔离实例已正常停止，失败现场保留；浏览器清理时连接中断，旧压力标签关闭/视口重置未确认。具体证据和性能例外见月级数据说明。无Git写操作，不开始下一阶段。
