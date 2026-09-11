# v1.2-weather-ui-refine：前端气象数据展示优化与视觉收口

归档日期：2026-09-11。**WEATHER-UI-REFINE-1 COMPLETED**；Gate：**READY FOR FINAL-ACCEPTANCE**。分支：`feat/weather-ui-refine`。本归档不表示已提交 Git、创建 tag 或生成完整发布包；本轮不开始 FINAL-ACCEPTANCE。

## 完成范围

保留深蓝色气象工作台风格，在现有前端组件及 API 契约内优化月度数据展示。

| 页面 | 已完成功能与人工验收结果 |
| --- | --- |
| /weather | 日期 + 当天实际时次正常；默认 30 天月度范围并保留旧兼容样例快捷入口；切日优先保留钟点，仅使用真实返回时次；六要素切换、地图/图例/城市详情联动与布局正常 |
| /analysis | 30 天长序列、slider + inside dataZoom、完整日期时间和单位 tooltip 正常；保留 T2M/PRECIP 两图与原四项统计含义 |
| /comparison | ECMWF / NOAA 视觉区分、六要素选择、dataZoom、双模型 tooltip 正常；null 与 0 正确区分，缺测不补零 |

六要素统一前端名称、单位、精度、图例和展示类型：T2M（℃）、PRECIP（mm）、TCC（%）、WIND_SPEED_100M（m/s）、WIND_DIR_100M（°）、RH（%）。风向显示角度 + 中文八方位，图例明确 0°/360° 北、90° 东、180° 南、270° 西，颜色表示方位；TCC/RH 固定 0–100% 语义。

页面空间、控件与图表阅读层级已调整；管理 CRUD 未重新设计，100 条本地分页策略保留。

## 认证回归

- USER 仅显示气象工作台、趋势分析、模型对比，不显示数据管理。
- ADMIN 保留三业务页面和数据管理入口。
- logout 后再次访问受保护页面返回 `/login`。
- Spring Security + HttpSession（Session）+ BCrypt + CSRF、数据库角色权威及路由权限规则未修改。
- 原 19 个天气业务 API 与另计的 4 个 auth API 均未修改，未新增后端接口。

## 最终验收证据

下表区分前阶段自动化结果与用户提供的人工验收；**本收口轮没有重新运行测试、构建或浏览器验收**。

| 检查 | 结果 | 证据来源 |
| --- | --- | --- |
| frontend npm test | **193 PASS，0 failures**（Node 120 + Vitest 73） | 前阶段实际执行结果 |
| frontend npm run build | **PASS** | 前阶段实际执行结果 |
| git diff --check | **PASS** | 前阶段实际执行结果；收口时另检查文档差异 |
| USER 浏览器验收 | **PASS** | 用户本次人工确认 |
| ADMIN 浏览器验收 | **PASS** | 用户本次人工确认 |
| 1920×1080 | **PASS** | 用户本次人工确认 |
| 1366×768 | **PASS** | 用户本次人工确认 |
| 布局 | **无阻塞性布局问题** | 用户本次人工确认 |

此前 Codex 浏览器工具不可用，阶段暂记 `RUNTIME_VALIDATION_PENDING / MANUAL_BROWSER_VALIDATION_REQUIRED`。用户现已完成上述真实人工验收，待验收项已补齐，阶段状态更新为 COMPLETED；不将人工证据描述为 Codex 新运行的测试。

## 后端与数据库保护

**Backend Modified = NO；Database Modified = NO**。沿用已确认的开发数据库基线，本轮未重新查询数据库：

| 项目 | 已确认数量 |
| --- | ---: |
| city | 16 |
| forecast_model | 2 |
| weather_element | 6 |
| forecast_record | 46080 |
| distinct forecast_time | 240 |
| sys_user | 2 |

数据仍为 16 城市 × 2 模型 × 6 要素 × 30 天 × 每天 8 时次；未修改 SQL、生成算法、认证种子、后端业务代码或 Spring Security。backend 365 PASS 是 AUTH 阶段历史基线，本轮未重跑后端测试。

## Known Issue

构建存在 **>500 kB chunk warning**。当前不影响运行与课程设计验收，本阶段不处理，不将此提示写为构建失败。

## 收口边界

- 本轮仅更新 CURRENT_CONTEXT、RESUME，并新增本 README；前阶段已有前端实现保留不变。
- 不修改 frontend 业务代码、backend、SQL、authentication、portable 或 package 配置；Tests Re-run=NO。
- [v1.0-final](../v1.0-final/README.md) 和 [v1.1-auth-role](../v1.1-auth-role/README.md) 历史归档保持原文，不复制源码或构建产物。
- Git Writes=NONE；不暂存、提交、推送、切换分支或创建 tag。
- READY FOR FINAL-ACCEPTANCE 仅表示就绪，等待用户单独指令；不自动执行下一阶段。

当前状态：[CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)、[RESUME](../../课程设计/docs/context/RESUME.md)。
