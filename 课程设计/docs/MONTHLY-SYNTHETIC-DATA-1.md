# 月级课程合成气象数据

## 当前授权范围

MONTHLY-SYNTHETIC-DATA-1 在 v1.0 软件基础上扩展演示数据，不接入第三方气象 API，不训练预报模型，不实现登录权限或整体美化。真实大表不可用触发用户第十四条例外，只增加预报记录本地分页，后端仍全量GET。历史 v1.0 归档和中期报告保留原验收口径。

冻结目标：山东16市、ECMWF/NOAA两个模型场景、六要素、2026-09-01～30每日02/05/08/11/14/17/20/23，共240时次，理论46,080条。实际生成与数据库数量必须另行验收，不以公式替代实数。

| 编码 | 名称 | 单位 | 来源 |
| --- | --- | --- | --- |
| T2M | 2米气温 | ℃ | 合成天气过程直接构造 |
| PRECIP | 降水量 | mm | 同一过程构造三小时时段量 |
| TCC | 总云量 | % | 同一过程直接构造 |
| WIND_SPEED_100M | 100米风速 | m/s | u100、v100 推导 |
| WIND_DIR_100M | 100米风向 | ° | u100、v100 推导 |
| RH | 相对湿度 | % | T2M、内部露点 d2m 推导 |

u100、v100、d2m 仅为生成器内部变量，不写入 weather_element。两个模型共享基础天气过程并增加小偏差，不是两套无关随机数，更不是真实 ECMWF 或 NOAA 数据。

## 推导公式与语义

风速为 sqrt(u100²+v100²)。风向使用 Math.atan2(-u100,-v100)×180/π，正规化到 [0,360)。u 向东为正、v 向北为正；风向是风的来向，以北为0°、东90°、南180°、西270°。圆周连续性应比较最短角距离，不能把359°到1°当成358°突跳。

来源：[NCAR/EOL Wind Direction Quick Reference](https://www.eol.ucar.edu/content/wind-direction-quick-reference)。采用编程语言 atan2(y,x) 参数顺序，不套用电子表格的反向参数约定。

RH 采用水面饱和水汽压的 Magnus 型近似：es(T)=6.112×exp(17.67T/(T+243.5))；由露点定义 e=es(D)，RH=100×exp(17.67D/(D+243.5)-17.67T/(T+243.5))，输出约束到0～100。T、D均为℃；生成内部 D 不高于 T。这里是正温课程场景的近似，不是冰面湿度算法或真实观测反演链路。

系数与 RH=100e/es 的依据：[美国国家气象局计算说明](https://preview.weather.gov/media/epz/wxcalc/rhTdFromWetBulb.pdf)。该资料给出饱和水汽压、相对湿度与露点关系；本生成器不使用其湿球温度输入，而由合成露点代入饱和水汽压比值。

PRECIP 每点表示以该时间为终点的前三小时非重叠时段量。例如05:00对应02:00～05:00；9月1日02:00对应8月31日23:00～9月1日02:00。不是起报累计降水。查询闭区间选择记录终点；不能把三个时次简单称为六小时总量。

## 兼容规则

四表、字段、外键和唯一键不变：NO_SCHEMA_CHANGE_REQUIRED。Workbench、Comparison支持六要素；Trend仍返回T2M/PRECIP与四项统计。十九个业务API不增减。

JINAN 2026-09-07 08/11/14的十二个温度和降水验收值保留。生成器如覆盖基础过程，明确标注“兼容 v1.0 固定验收样例”；RH须根据最终温度与内部露点重新推导，不能保留不相容的旧衍生值。

范围约束属于合成数据生成目标，不是山东历史极值，也不自动改成全体CRUD的业务上限。既有零值、缺测、时间格式、BigDecimal统计、唯一与引用保护保持。

## 初始化与安全

开发期生成最终UTF-8批量SQL，目标电脑无需Node生成数据。portable继续依次使用原schema.sql和新的data.sql，只在新目录首次初始化导入；后续启动保留数据，不自动迁移旧v1.0数据库。旧运行目录不得简单覆盖SQL后重跑schema。

本轮选择新建隔离实例验证，不使用开发库3306，不复制开发凭据，不制作完整发布ZIP。46,080行管理列表保持全量接口；初始真实浏览器渲染约35秒并发生交互超时，因此按用户明确例外做最小100条本地分页，不扩后端接口。

## 验证记录

固定 seed：20260901。生成器10组行为测试覆盖用户22项要求；实际解析SQL为46,080条唯一业务键，两次CLI结果与database/data.sql字节一致。SHA256：`3F22D495B6C4505A72D0FBFFBAD319A028CEB3D560E6AB662EAA0CF99F270C37`。

真实新建隔离库：16/2/6/46,080，240时次、30天每天8时次，192个city/model/element组合各240条，重复键0，首末2026-09-01 02:00:00和2026-09-30 23:00:00。每要素7,680条；PRECIP中6,253条为真实零。T2M 15.18～30.17℃、PRECIP 0～7mm、TCC 13～100%、风速3.13～13.99m/s、风向148.70～281.74°、RH56.24～95.69%。这些只是本次合成样本范围。

数据库抽样：济南/青岛/烟台×ECMWF/NOAA×六要素，核验9/2晴、9/7雨、9/10风过程。例如济南ECMWF在三日14时的TCC为14.76/96.61/13.03%，RH为58.23/89.06/57.59%，风速为3.31/5.12/13.24m/s。六个城市模型组合有雨时TCC均值79.51～82.03%，无雨23.02～25.24%；有雨RH84.49～86.02%，无雨59.55～60.59%。相邻三小时风速最大变化0.90m/s、风向最短角变化3.23°。固定十二个济南样例逐值通过。此为合成合理性，不是预报准确性。

真实HTTP：Workbench六要素各240时次/3,840记录；Comparison六要素各两个模型、每模型240点；Trend仍两序列各240点，默认窗口四统计21.10/19.00/20.10/0.60。管理GET实际46,080条、11,249,703字节，本机一次读取/解析202ms（不是端到端浏览器指标）。

首次启动在沙箱中因CIM进程身份查询权限不足而失败；已核实归属并用该实例私有客户端正常关闭，失败目录保留。使用新的隔离目录与原portable启动脚本重新初始化成功；没有覆盖旧目录。完整package第一次312项中1项旧193数量断言失败，修正为46,080+1后312/0/0/0与repackage通过。后续兼容修复与最终页面验收完成后再补最终总数，不以本段中间结果宣称任务完成。

## 硬编码审计分类

- A 真正业务规则：Trend固定T2M/PRECIP及四统计；PRECIP负数拒绝与零合法；16市/ECMWF/NOAA核心筛选；保留。
- B 默认演示值：JINAN、ECMWF、T2M及9/7 08～14集中默认窗口；保留，不代表仅有3时次。
- C 固定测试样例：Mock的两字典/三点、济南十二值、各种异常/缺测/零值测试；保留，不扩大为全月样例替代业务断言。
- D 已过时限制：application.yml要素白名单、Workbench/Comparison前端白名单与Comparison单位校验扩六种；真实DB的2元素/192记录更新6/46,080，CRUD新增后193更新46,081；旧9/8无数据窗口移到10月。没有全局替换T2M/PRECIP。

## 最终验收（2026-09-11）

状态：MONTHLY-SYNTHETIC-DATA-1 COMPLETED。Gate：READY FOR AUTH-ROLE-1；仅为下一阶段建议，本轮未开始权限或美化，也未创建v1.1归档/ZIP。

- TDD来源为本轮实际执行：生成器初始8项RED后完善为10项GREEN；六要素前端新增6项先RED，后端Controller新增8项先RED。Runtime修复的图层2项最初1失败、分页14项最初3失败，随后全过。默认配置指定图层spec曾返回“No test files found/code 1”，接入默认include后修复；删除本轮冗余的局部Vitest配置，不删除业务测试。
- 最终generator：10 passed / 0 failed / 0 skipped；再次实际运行并校验SQL哈希。
- 最终frontend：npm test PASS，Node98 + Vitest59 = **157 passed / 0 failed / 0 skipped**，包括图层2项和管理分页3项。npm run build PASS；图层测试入口配置修改不影响此前最终业务代码的构建产物。
- 最终backend：先test-compile通过；再用本轮隔离库实际执行 `mvn -o -Pportable package`（本地依赖已齐，无下载/升级），**324 tests / 0 failures / 0 errors / 0 skipped**，JAR与repackage成功。包含六要素Workbench/Comparison新增12项真实月级Mapper测试及原CRUD事务回滚测试。
- 本轮真实浏览器使用合并JAR、127.0.0.1:18080与隔离MySQL13306，不是Vite或Mock。/weather六要素逐项点击，地图均16/16有值，济南T2M19.00、PRECIP0.00、TCC98.42、风速5.10、风向153.12、RH92.53（9/7 08时ECMWF）；单位同步。修复后的1366×768和960×768图层面板可滚动/换行，首末选项可达；不声称全面移动端美化完成。
- /comparison六要素逐项点击查询，均显示ECMWF/NOAA双折线及对应℃/mm/%/m/s/°；/analysis原温度线与降水柱保留，默认四统计21.10/19.00/20.10/0.60。图片与页面状态为本轮真实后端证据。
- /management真实46,080条：修复后页签加载到DOM核验不足1秒，100行/页，共461页；第二页从ID101开始，页面无横向溢出，刷新按钮恢复响应。此为一次本机工具观察而非性能基准。翻页不发GET、205/201条测试集跨页、末页删除回退由组件自动测试覆盖，未冒充真实数据库删种子验收。本轮不做HTTP写入；数据库集成测试使用事务回滚。
- 正常页面验收期间Console错误/警告列表为空；之后在关闭旧大表压力标签时浏览器连接中断，旧标签关闭与临时视口重置未获确认，可能需要用户手动关闭。该工具清理问题不倒推此前页面验收失败，也不宣称清理成功。
- 真实SQL最终复查仍16/2/6/46,080、240时次、192组各240条、重复0。新隔离目录`.portable-build/monthly-20260911-verified`使用原portable初始化链路直接导入新SQL，后续替换JAR重启保持数据；最终正常关闭，本轮13306/18080无监听。首次失败目录`.portable-build/monthly-20260911`保留，未删除以往失败现场。没有连接或修改开发库3306。
- 只读独立审查发现图层遮挡、全量DOM风险与新增测试入口遗漏，均经本轮实证和最小修复收口；最终未发现重要遗留代码问题。Schema和历史versions/v1.0未改，中期检查既有文件未覆盖。私有凭据、数据库目录、日志及JAR均在已忽略的运行目录/构建目录，未进入Git待提交范围。

## 仍有的限制

全量记录接口仍约11.25MB，本地分页仅限制DOM，不减少网络数据；没有搜索、后端分页或任意页跳转，远端/低配性能须后续评估。共享前端chunk大于500kB警告保留。风向继续使用现有双折线/标量色标，未实现风矢量或圆周图；本次生成范围未跨0°。旧v1.0发布ZIP仍为旧数据，正式v1.1包尚未重制，目标笔记本运行不在本轮声明范围。本系统为合成课程演示，无认证，不应将管理写接口公开暴露。

## 本轮实际文件与Git检查

以下41个受跟踪修改或新增文件属于本轮；既有未跟踪“课程设计/中期检查/”保留，不计入本轮。`课程设计/.DS_Store`仍被跟踪，未改动，仅列既有清理项。最终git diff --check通过，修改文件冲突起止标记0，未发现新增凭据、日志或生成物进入待提交范围；Schema与versions目录无差异，Git writes = NONE。

- `课程设计/00-项目总览/项目当前状态.md`
- `课程设计/00-项目总览/README.md`
- `课程设计/01-立项与需求/08-数据需求.md`
- `课程设计/01-立项与需求/09-需求冻结确认.md`
- `课程设计/02-系统设计/02-功能模块设计.md`
- `课程设计/02-系统设计/04-接口设计.md`
- `课程设计/03-数据库设计/05-数据字典.md`
- `课程设计/03-数据库设计/08-SQL设计说明.md`
- `课程设计/AGENTS.md`
- `课程设计/docs/context/CURRENT_CONTEXT.md`
- `课程设计/docs/context/RESUME.md`
- `课程设计/docs/MONTHLY-SYNTHETIC-DATA-1-plan.md`
- `课程设计/docs/MONTHLY-SYNTHETIC-DATA-1.md`
- `课程设计/README.md`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/shandong/weather/ComparisonMapperIntegrationTests.java`
- `backend/src/test/java/com/shandong/weather/DatabaseConnectionTests.java`
- `backend/src/test/java/com/shandong/weather/DictionaryMapperIntegrationTests.java`
- `backend/src/test/java/com/shandong/weather/ManagementCrudIntegrationTests.java`
- `backend/src/test/java/com/shandong/weather/TrendMapperIntegrationTests.java`
- `backend/src/test/java/com/shandong/weather/WeatherComparisonControllerTest.java`
- `backend/src/test/java/com/shandong/weather/WeatherQueryControllerTest.java`
- `backend/src/test/java/com/shandong/weather/WorkbenchMapperIntegrationTests.java`
- `database/data.sql`
- `database/README.md`
- `frontend/src/components/management/ForecastRecordManagement.spec.js`
- `frontend/src/components/management/ForecastRecordManagement.vue`
- `frontend/src/components/weather/WeatherSwitchers.spec.js`
- `frontend/src/components/weather/WeatherSwitchers.vue`
- `frontend/src/utils/comparisonAnalysis.js`
- `frontend/src/utils/comparisonAnalysis.test.js`
- `frontend/src/utils/comparisonState.js`
- `frontend/src/utils/comparisonState.test.js`
- `frontend/src/utils/weatherElements.js`
- `frontend/src/utils/workbenchState.js`
- `frontend/src/utils/workbenchState.test.js`
- `frontend/vitest.config.js`
- `tools/data/cities.mjs`
- `tools/data/generate-monthly-weather-data.mjs`
- `tools/data/generate-monthly-weather-data.test.mjs`
- `tools/portable/README.md`
