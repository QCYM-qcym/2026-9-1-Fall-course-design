# v0.9 — FE-TREND-1

> 收口日期：2026-09-08。状态：FE-TREND-1 COMPLETED。
> Gate：READY FOR NEXT PHASE。

## 目标与范围

完成 /analysis 趋势分析前端闭环，连接真实 Spring Boot + MySQL Trend API，展示城市/模型查询、温度折线、降水柱状及四项后端统计，并提供可验证的加载、空结果、失败重试、竞态保护和响应式界面。

只扩展本阶段前端；不修改 backend、database、冻结接口或历史归档，不实现 Comparison、Management、CRUD 或地图新功能。本目录仅保存阶段说明，不复制源码。

## 页面与组件结构

[TrendAnalysis.vue](../../frontend/src/views/TrendAnalysis.vue) 复用已有路由 /analysis、App Shell 和深色气象工作台风格，布局依次为筛选区、四统计卡、温度图、降水图及样本说明。

| 单元 | 职责 |
| --- | --- |
| [TrendFilterBar](../../frontend/src/components/analysis/TrendFilterBar.vue) | 城市、模型、DateTime Range Picker、查询按钮 |
| [TrendStatistics](../../frontend/src/components/analysis/TrendStatistics.vue) | 四个后端统计字段的展示 |
| [TrendChart](../../frontend/src/components/analysis/TrendChart.vue) | line/bar 两实例、更新、resize、dispose 及图表状态 |
| [trendState](../../frontend/src/utils/trendState.js) | 字典、查询、Loading/Empty/Error/Retry、requestVersion |
| [trendAnalysis](../../frontend/src/utils/trendAnalysis.js) | 参数与响应校验、数值/时间格式、统计映射、图表选项 |

沿用 Vue 3.5.30、Vite 7.3.1、Vue Router 4.6.4、Element Plus 2.13.3、ECharts 6.0.0、Axios 1.13.6 和 JavaScript；不新增依赖或测试框架。

## 三个 API 与筛选

- GET /api/cities：城市字典。
- GET /api/forecast-models：模型字典。
- GET /api/weather/trend：cityId、modelId、startTime、endTime 四参数；复用现有 Axios /api 实例，不硬编码后端地址。

两个字典并行加载，按 cityCode=JINAN、modelCode=ECMWF 选择默认值，不依赖字典顺序或固定 ID。缺少默认编码时使用可用核心选项；无可用选项明确报错，不伪造字典。

默认范围复用集中 DEMO_RANGE：2026-09-07 08:00:00～14:00:00。初始化成功后自动查询一次，之后修改筛选仅更新草稿，点击查询才请求；本阶段无 Cache。时间保留 yyyy-MM-dd HH:mm:ss 本地业务字符串，不转换 UTC。未提交的筛选变化显示提示，结果标题始终标明已查询城市/模型/范围。

## 图表、统计与缺测语义

- Temperature：ECharts line，使用 temperature 的 forecastTime/value，单位 ℃。
- Precipitation：ECharts bar，使用 precipitation 的 forecastTime/value，单位 mm。
- 两序列各自独立，不按下标绑定，不补零或伪造缺测点；tooltip 包含本地时间、值与单位。
- Statistics 只读取 temperatureMax、temperatureMin、temperatureAvg、precipitationTotal；前端不从序列重新计算。
- 有限数值显示两位小数；真实 0 显示 0.00，null/undefined 显示 --。
- 页面提示统计仅基于返回样本。两个图表分别 init，查询后 setOption，不反复初始化；resize 响应窗口与容器变化，卸载 dispose、移除窗口监听并断开 ResizeObserver。

## 状态与竞态保护

- Loading：字典或 Trend 请求期间保留页面框架，清除旧结果，显示加载状态，不白屏。
- Empty：合法 HTTP 200、两个空数组显示“当前条件暂无趋势数据”，统计 --，不显示 Error/NaN/undefined。单要素有值不当成整页 Empty。
- Error：提供可见错误 UI，不展示原始服务端详情。
- Retry：使用当前 cityId/modelId/startTime/endTime，不重置筛选；字典失败可重新加载字典。
- Race Protection：requestVersion 使旧成功、旧失败、无效新提交前的旧请求及卸载后的响应失效；最后一次查询控制最终结果，不引入复杂请求框架。

## Runtime Acceptance

以下均依据用户本次明确确认的真实 Spring Boot + MySQL 人工浏览器验收，不冒充 Codex 收口轮直接采集，不声明服务当前持续在线。实现轮后端离线及截图裁切导致的待验收项，现由用户真实验收补齐。

默认：济南 + ECMWF，2026-09-07 08:00～14:00，两个系列各 3 点。

| 时次 | Temperature（℃） | Precipitation（mm） |
| --- | --- | --- |
| 08:00 | 19.00 | 0.00 |
| 11:00 | 20.20 | 0.40 |
| 14:00 | 21.10 | 0.20 |

四项统计：temperatureMax=21.10、temperatureMin=19.00、temperatureAvg=20.10、precipitationTotal=0.60。

| 验收项 | 用户真实结果 |
| --- | --- |
| Default | PASS：默认城市、模型、范围、双系列及四统计一致 |
| City Switch | PASS：JINAN → QINGDAO，两图及四统计刷新，无济南残留 |
| Model Switch | PASS：ECMWF → NOAA，图表与统计刷新 |
| Loading | PASS：可见、框架保持、无白屏 |
| Empty | PASS：无数据日期 HTTP 200、Empty UI、四项 --，无 Error/NaN/undefined |
| Error / Retry | PASS：后端断开进入 Error，恢复后 Retry 成功，筛选保留 |
| Race | PASS：快速模型/筛选切换，最终对应最后一次请求，无旧响应覆盖 |
| 1366×768 | PASS：筛选、统计、图表正常，无横向溢出 |
| 1920×1080 | PASS：筛选、统计、图表正常，无横向溢出 |
| Console | PASS：0 Uncaught Error、0 Unhandled Promise、0 Vue warning、0 ECharts disposed error |
| /weather Regression | PASS：山东地图、ECMWF/T2M、Timeline、City Detail 正常，Trend 样式未污染 Workbench |

## 自动测试与构建

实现轮遵循 TDD：原 20 项测试保持通过，新 25 项先因功能缺失失败，再实现并转绿。使用现有 node:test，不引入第二测试框架。覆盖真实 API 客户端参数、默认编码、时间格式、图表选项与 ECharts SVG 渲染/更新/resize/dispose、四统计字段、零值/空值、双序列时次不齐、Loading/Empty/Error/Retry、旧成功/失败及卸载失效。

- npm test：45 passed / 0 failed / 0 skipped（原 20 + 新 25）。
- npm run build：PASS；2125 modules，Trend 路由 JS 82.46 kB/gzip 29.37 kB，主 JS 1345.66 kB/gzip 447.95 kB。
- 独立只读代码审查：无 Critical/Important/Minor；审查者复跑 45 项通过。
- 本轮仅文档收口，沿用上述实际测试/构建结果，未重跑命令或浏览器。
- Backend baseline：134 tests PASS、Package PASS、Trend Runtime PASS，依据 [v0.8-be-trend](../v0.8-be-trend/README.md) 已确认的真实证据；本阶段 backend/database 未修改。

## Known Limitations

- 当前 Seed 仅 3 个固定合成演示时次、192 条记录，不代表完整 7 天或实时气象数据。
- 本阶段不实现缓存、CRUD、Comparison 或 Management；下一阶段须另行审计和授权。
- 大 chunk 为 KNOWN NON-BLOCKING MINOR，不在本阶段扩展优化。
- 课程设计/.DS_Store 仍为已跟踪生成文件，独立非阻塞清理项；未自行移除。

## 安全与 Git

不记录凭据，不创建 .env/.mylogin.cnf，不硬编码后端 URL；构建产物、日志、临时截图和浏览器导出文件不进入待提交。本次收口仅同步 [CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)、[RESUME](../../课程设计/docs/context/RESUME.md) 并创建本档案，业务代码及历史归档不改动。

- Branch：feat/fe-trend。
- 开始基线 HEAD 与本地 origin/main：4a528eee9866e752fb3a11dfe2e3184be0e435b1；这是起始基线，不是本阶段完成提交。
- 完成 commit/tag/push：不虚构，未由 Codex 创建或执行。
- Git writes：NONE；用户通过 GitHub Desktop 人工检查、暂存、提交和推送。

## 下一阶段

FE-TREND-1 COMPLETED；READY FOR NEXT PHASE。

建议先审计 Comparison Backend，不直接假设 /api/weather/comparison 已实现。本轮不开始审计或下一阶段开发。
