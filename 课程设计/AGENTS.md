# AGENTS.md

## 1. 项目定位

本仓库维护两人、两周的课程设计项目《山东省气象预报数据可视化系统的设计与实现》。目标是在已冻结的需求和数据库设计上完成可演示、可检查、可复现且交互清晰的 V2 系统，控制两人两周工程量。

本文件适用于仓库根目录及全部子目录。开始任务前，先阅读本文件、`docs/context/CURRENT_CONTEXT.md`、`docs/context/RESUME.md`；具体任务还应以用户当轮指令为准。

## 2. 冻结方案

- 技术栈：Vue 3、Vite、JavaScript、Element Plus、ECharts、Axios、Vue Router；Java 17、Spring Boot 2.7.18、MyBatis-Plus、MyBatis XML、Maven 3.9.16；MySQL 8.0.16+。
- 业务模块：Weather Workbench、Trend Analysis、Model Comparison、Data Management。
- 核心表：`city`、`forecast_model`、`weather_element`、`forecast_record`。
- 业务唯一键：`(city_id, model_id, element_id, forecast_time)`。
- 后端接口分 CRUD 与 WeatherQuery 两类；五 Controller、五 Service、四 Mapper，展示查询用 ForecastRecordMapper.xml。
- 初始化范围：山东全部 16 地级市；核心演示济南/青岛，模型 ECMWF/NOAA，要素 T2M（2 米气温，℃）/PRECIP（降水量，mm）；实际为 2026-09-07 08:00、11:00、14:00 三个时刻、192 条合成课程演示数据，不是实时气象服务；不增加新业务表。
- 山东地图属于 P0，采用 ECharts Map + 静态 GeoJSON；Workbench 批量加载所有城市与全部时刻，时间轴只本地切换，不逐次请求后端。
- 前端可用简单内存缓存，CRUD 成功使缓存失效；缺测不补零，同页表格、图表和统计来自同次响应。
- Trend 同时展示温度折线、降水柱状和四项统计；Comparison 只做 ECMWF/NOAA 视觉对比，四类对象完整 CRUD。

## 3. 不扩展范围

除非用户明确要求并先更新需求冻结记录，否则不得引入：

- 登录、用户、权限、JWT；
- Redis、TDengine、微服务；
- NetCDF 在线解析、ERA5 或实时气象数据接入；
- 复杂预测分析、生产级部署；
- 超出四张核心表的新业务实体；
- 复杂 GIS（Leaflet/Mapbox/Cesium/PostGIS/GIS Server）、风场动画、高空场、RMSE/MAE/bias/accuracy、预测训练和自动交易。

本课程设计当前地域范围固定为山东省，未经用户明确批准，不得重新调整省份或扩展为全国范围。

不得把规划、示例、待执行 DDL 或接口草案写成已经实现。发现现有文档与实际文件不一致时，以可验证的文件和运行结果为准，并明确记录差异。

## 4. Monorepo 与职责边界

项目采用一个仓库管理前端、后端、数据库和文档。规划目录职责如下：

- `frontend/`：页面、组件、路由、图表和 API 调用，不直连数据库。
- `backend/`：REST API、业务校验和数据访问；Controller 不直连 Mapper。
- `database/`：DDL、初始化数据和数据库实施说明。
- `课程设计/`：唯一当前课程设计文档，含编号资料、docs/context/ 上下文与 Canvas；不另建根级 docs/ 重复文档体系。
- `versions/`：独立阶段记录，不是源码副本。

跨层字段变更必须同步检查数据库、Entity、Mapper、Service、接口契约、前端映射、数据字典和相关图示。前端不得长期依赖自行假设的接口字段。

## 5. Version Archive Rules

项目采用两层版本管理：

- `frontend/`：保存当前最新前端代码；
- `backend/`：保存当前最新后端代码；
- `database/`：保存当前最新数据库脚本；
- `课程设计/`：保存当前有效文档；
- `versions/`：保存每个可独立验收阶段的版本档案。

每完成一个可独立验收的小版本，应在 `versions/` 下创建对应的 `v0.x/` 或 `v1.0/` 目录。版本档案主要保存：

- `README.md`；
- 阶段目标与完成功能；
- 前端、后端和数据库状态；
- 测试结果与已知问题；
- 截图、接口变化摘要或数据库变化摘要；
- 关联的 Git branch、commit 和 tag。

禁止为了版本留档而复制整套 `frontend/`、`backend/` 或 `database/` 源文件，也不得复制 `node_modules/`、`target/` 等构建产物。真实代码历史由 Git commit 和 Git tag 保存。

工程及四页面、19 个业务 API 已实现，阶段归档已至 v0.13；最终验收状态以 CURRENT_CONTEXT 为准。本地开发配置不等于生产部署配置。

## 6. Git 协作规则

- 稳定主分支为 `main`，开发使用 `feat/*`、`fix/*`、`docs/*`、`test/*` 短期分支。
- 不设置长期 `develop`、`release`、`hotfix`、`frontend` 或 `backend` 分支。
- 一个分支和一次提交只处理一个清晰目标；提交信息采用简化 Conventional Commits。
- 团队统一使用 `merge` 同步，不混用 `merge` 和 `rebase`。
- 提交前查看状态，仅暂存本任务文件并检查暂存差异；默认禁止无检查执行 `git add .`。
- 禁止擅自执行 `git reset --hard`、强制推送、删除未合并分支、覆盖用户修改或其他破坏性 Git 操作。
- 未经明确要求，不创建分支、不提交、不推送、不改写历史。

## 7. Codex 开始任务检查

1. 确认当前路径和任务允许修改的文件。
2. 按顺序阅读本文件、`docs/context/CURRENT_CONTEXT.md`、`docs/context/RESUME.md`，再阅读任务相关文档。
3. 检查 `git status --short`；若不是 Git 仓库，如实说明，不自行初始化。
4. 查找同名文件与现有实现，避免重复创建或覆盖。
5. 阅读与任务直接相关的冻结文档、接口契约或数据字典。
6. 区分“已实现”“已设计”“待执行”，不根据目录规划推断完成状态。
7. 若任务会改变冻结范围且用户尚未明确授权，先请求确认；用户最新明确要求优先。实际代码与文档冲突时以代码为准并报告。

## 8. 最小变更原则

- 只修改完成当前任务所必需的文件，不顺手重构或统一格式。
- 保留用户现有内容和未提交改动，不改动无关文件。
- 优先复用已确认的命名、字段、接口和目录规则。
- 不添加无必要依赖、抽象层、配置或兼容逻辑。
- 文档使用可在 GitHub 直接阅读的 Markdown；链接使用仓库相对路径。
- 任何密钥、真实密码、令牌、本机私有配置和无必要绝对路径都不得写入仓库。

## 9. 测试与验证

- 测试范围应与改动风险相称：先运行直接相关检查，再决定是否扩大验证。
- 后端改动至少检查编译和相关测试；前端改动至少检查构建及相关测试；数据库改动检查 DDL、约束、索引和初始化顺序；文档改动检查链接、Markdown 和事实一致性。
- 涉及核心查询时，覆盖济南/青岛切换、ECMWF/NOAA、T2M/PRECIP、时间范围、无结果和重复数据边界。
- 涉及图表时，验证图表与摘要来自同一响应；Trend 为 T2M 折线与 PRECIP 柱状，Comparison 两要素均为 ECMWF/NOAA 双折线，地图色标与单位正确；另查时间轴零新增请求与写入后缓存失效。以已批准实现为准，不恢复早期明细表、Comparison 柱状图或管理筛选规划。
- 不得声称未运行的测试通过。因缺少工程、依赖、数据库、Git 仓库或环境而无法执行时，必须标记为“未运行”并说明原因。

## 10. 完成报告格式

每次任务结束时简要报告：

1. 完成内容：实际创建或修改了什么。
2. 修改文件：逐项列出路径。
3. 验证结果：列出已运行检查及结果。
4. 未运行检查：列出未运行项及原因。
5. 范围确认：说明是否触及冻结范围、无关文件、提交或推送。
6. 后续事项：仅列真实剩余工作或阻塞，不把建议写成已完成。
