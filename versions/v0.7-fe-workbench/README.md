# v0.7 Frontend Weather Workbench

## 目标与状态

FE-WORKBENCH-1 COMPLETED，收口日期：2026-09-08。

在既有 Vue 3 工程中完成 `/weather` 气象工作台，连接真实 Spring Boot + MySQL 接口，以山东地图、时间轴及城市详情展示课程合成预报数据。不修改后端、数据库、API 契约或其他业务页面，不复制源码到版本目录。

Gate：READY FOR TREND BACKEND AUDIT。本阶段按用户最新人工验收确认与收口要求完成；以下明确区分真实浏览器证据、自动测试与未单独验证项，不表示早前全部细分 Runtime Gate 已通过。

## 页面与组件结构

[WeatherWorkbench.vue](../../frontend/src/views/WeatherWorkbench.vue) 组合六个组件，复用现有 App Shell、Axios `/api` 与 Vite proxy。

| 组件 | 职责 |
| --- | --- |
| WeatherToolbar | 标题、当前模型/要素、查询日期范围 |
| WeatherSwitchers | ECMWF/NOAA 与 T2M/PRECIP 切换 |
| ShandongWeatherMap | ECharts Map、16 市区域和点击、ResizeObserver 与销毁 |
| WeatherLegend | 当前时次动态图例、单位与色标 |
| CityDetailPanel | 城市选择、数值、模型、要素、时次、坐标及关闭 |
| ForecastTimeline | 08/11/14 时次切换，仅更新本地选择 |

组件位于 [components/weather](../../frontend/src/components/weather)。[workbenchState.js](../../frontend/src/utils/workbenchState.js) 管理查询状态、缓存及请求版本；[weatherWorkbench.js](../../frontend/src/utils/weatherWorkbench.js) 管理映射、关联、过滤和色标；[mapOption.js](../../frontend/src/utils/mapOption.js) 构建地图选项。未新增依赖。

## GeoJSON 来源、许可与匹配

来源为 Supeset/China-GeoData 的 `geojson/china_province_city_full.geojson`，MIT 许可；获取日期 2026-09-08。仅筛选山东 `adcode` 前缀 37 的 16 个 Polygon/MultiPolygon feature，未手绘、简化或修改坐标。版权和完整许可见 [地图资源说明](../../frontend/src/assets/maps/README.md)。

16 features / 16 matched / 0 unmatched；城市编码显式对应 GeoJSON 名称，再以真实字典 id 关联 Workbench 记录。几何检查记录：49 个闭合环、7153 个点。此前 Codex 实际点击济南、青岛、烟台区域映射正确；用户最终人工确认山东 16 市点击正常。资源仅用于课程示意，不用于测绘、导航或行政界线认定，不保证官方精度。

## 四个真实后端 API

采用此前已确认的真实 HTTP 结果，并结合用户最终页面验收；本轮收口不重新请求接口，不声明服务持续在线。

| GET | 已确认结果 |
| --- | --- |
| `/api/cities` | HTTP/code 200，16 市 |
| `/api/forecast-models` | HTTP/code 200，2 个模型 |
| `/api/weather-elements` | HTTP/code 200，2 个要素 |
| `/api/weather/workbench` | HTTP/code 200，3 times / 48 records |

默认按字典 code 选择 ECMWF + T2M，不写死数据库 ID。当前固定演示范围为 `2026-09-07 08:00:00`～`2026-09-07 14:00:00`，本地业务时间；数据为课程 Seed 合成样例，不是真实实时气象观测或预报。

## Timeline、Cache 与切换

- Timeline local filtering：PASS。一次 Workbench 响应覆盖 16 市及全部时次，切换只筛选本地 records。
- Timeline 0 additional Workbench request：PASS，依据用户本轮人工 Network 验收。
- Cache：PASS，依据用户人工回访验收与自动测试；页面此前实际回访显示“内存缓存”。key 包含 modelId、elementId、startTime、endTime，用户确认 Network 未见异常请求行为。
- Model / Element switching：PASS，ECMWF/NOAA、T2M/PRECIP 与 ℃/mm 同步切换；默认 ECMWF + T2M。
- City Detail：城市点击、当前值与时次同步；缺测不补零，真实零值保留。色标按当前时次动态计算，PRECIP 色标从零开始。

济南 ECMWF/T2M 实际浏览器值与用户最终确认一致：

| 时次 | 数值 |
| --- | --- |
| 08:00 | 19.00℃ |
| 11:00 | 20.20℃ |
| 14:00 | 21.10℃ |

请求竞态保护通过 requestVersion 实现，缓存命中也递增版本；旧成功/失败响应不得覆盖最新选择，卸载使未完成请求失效并清缓存。自动测试 PASS；本轮未单独取得真实请求乱序的浏览器证据，不将普通切换确认等同于完整竞态验收。

## Loading / Empty / Error / Retry 的真实状态

| 状态 | 自动测试证据 | 真实浏览器验收状态 |
| --- | --- | --- |
| Loading | 待决请求进入 loading、清除旧响应及失败结束状态的断言 PASS | 可见加载过程未单独验证，不标为 Runtime PASS |
| Empty | 成功空结果保留元数据、进入 empty 而非 error：PASS | 真实无记录范围的前端 Empty UI 未单独验证 |
| Error | 网络/字典失败处理及不暴露原始错误：PASS | 此前真实后端断开 Error State：PASS |
| Retry | 请求失败及字典失败后重试恢复：PASS | 此前离线重试不无限加载；真实后端恢复后点击按钮的 Error → Success 未单独验证 |

用户最终确认页面整体运行无明显问题，并明确要求按本轮范围完成收口；上述证据限制保留，不虚构 Loading / Empty / Retry 的独立人工确认。

## 浏览器、测试与构建

- 1366×768：此前真实有数据及城市详情打开场景 PASS，地图为主视觉，无严重遮挡；页面宽度/滚动宽度均为 1366，无横向溢出。
- 1920×1080：既有 DOM 无溢出检查通过，完整截图与视觉验收未单独完成，不标为视觉 PASS。
- Console：此前检查无错误；用户本轮确认整体运行无明显问题。本轮文档收口未重新操作浏览器。
- 最终收口 Codex 重新执行 `npm test`：20 passed / 0 failed / 0 skipped。覆盖 16 市映射、真实 ECharts SVG 渲染与销毁、缺测、图例、默认编码、时间轴、缓存、旧响应、空结果、错误重试和卸载失效；fixture 仅用于自动测试，非生产 Mock。
- 最终收口重新执行 `npm run build`：PASS，2116 modules。主 JS 1339.62 kB（gzip 445.77 kB）；Workbench 路由 377.77 kB（gzip 131.60 kB）。
- Backend 76-test baseline：PASS，采用用户此前真实 76 passed / 0 failures / 0 errors / 0 skipped；本阶段 backend/database 未修改，本轮未重跑 Maven，不冒充本轮执行结果。前置证据见 [v0.6-be-dictionary](../v0.6-be-dictionary/README.md)。

## Known Limitations 与安全

- 大 chunk：KNOWN NON-BLOCKING MINOR，本阶段不扩大优化范围。
- 简单内存 Map 缓存，无 TTL/LRU；离开页面清空。尚未实现 CRUD，后续管理写入需落实缓存失效，不宣称写入联动已完成。
- 当前 Seed 仅 3 个时次、192 条记录，约 7 天数据为后续规划。
- Loading / Empty / 恢复后 Retry、专门竞态浏览器证据及 1920×1080 完整视觉的限制见上文，不以自动测试冒充实际 UI 验收。
- Trend、Comparison、Management 仍为后续范围，本轮未开始。
- `课程设计/.DS_Store`：TRACKED_GENERATED_FILE，既有仓库清理项，按用户要求不阻塞 FE-WORKBENCH-1，未执行 git rm。
- 最终待提交清单未发现 node_modules、dist、.vite、.env、.mylogin.cnf、日志或临时文件；凭据不写入版本记录，不索取数据库密码。

## Git 与下一阶段

- Branch：feat/fe-workbench。
- 本次归档不记录或虚构完成 Commit、Tag、Push；代码及本档案尚未由 Codex 暂存、提交或推送，用户通过 GitHub Desktop 自行检查并操作。
- 本轮仅更新 [CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)、[RESUME](../../课程设计/docs/context/RESUME.md)，创建本档案；未修改业务代码或历史归档。
- 下一阶段建议：先审计 Trend Backend。Gate：READY FOR TREND BACKEND AUDIT，仅为建议，须用户另行授权，不自动开始 Trend。
