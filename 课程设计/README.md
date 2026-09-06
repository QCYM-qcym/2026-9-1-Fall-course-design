# 山东省气象预报数据可视化系统

课题名称：《山东省气象预报数据可视化系统的设计与实现》。本课程设计由两名成员在约两周内协作完成，V2 范围为 Weather Workbench、Trend Analysis、Model Comparison、Data Management 四个页面。

当前状态为“System Design V2 Freeze：UI、后端、数据库范围与 API V2 已冻结”。项目尚未开始正式编码、实际建库、接口联调或测试。本README中的工程目录、开发分支和Git命令均为后续开发规范或推荐示例，不表示对应目录、分支或功能已经存在。

## 文档入口

- [项目导航](00-项目总览/项目导航.md)：进入全部课程设计资料。
- [项目当前状态](00-项目总览/项目当前状态.md)：查看完成度、冻结内容和下一阶段。
- [需求冻结确认](01-立项与需求/09-需求冻结确认.md)：查看开发与验收必须遵守的冻结口径。
- [SQL设计说明](03-数据库设计/08-SQL设计说明.md)：查看待执行DDL、约束与初始化要求。
- [API V2](02-系统设计/04-接口设计.md)：唯一当前 19 接口契约。
- [前端 V2](02-系统设计/02-功能模块设计.md)：四页、组件、地图与 UI。
- [中期检查](05-测试与验收/01-中期检查方案.md) / [最终验收](05-测试与验收/02-最终验收方案.md)：V2 分层验收。
- [工程初始化计划 V2](06-项目管理/05-工程初始化计划.md)：当前初始化边界、测试门禁与中检优先级；计划已修订，工程尚未创建。
- [系统总体架构图](09-白板/03-系统总体架构图.canvas)：中期检查架构入口。
- [最终验收演示流程](09-白板/10-最终验收演示流程.canvas)：固定演示与验收流程。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、JavaScript、Element Plus、ECharts、Axios、Vue Router |
| 后端 | Java 17、Spring Boot 2.7.18、MyBatis-Plus、MyBatis XML、Maven 3.9.16 |
| 数据库 | MySQL 8.0.16+ |
| 文档与版本管理 | Markdown、Obsidian Canvas、Git、GitHub |

## 仓库策略

本项目计划采用 **Monorepo**：前端、后端、数据库和软件工程文档统一放在一个GitHub仓库中管理。

> **一个仓库，前后端目录分离；不设长期前端/后端分支，采用 `main + feat/fix/docs/test` 短期分支。**

前后端在目录结构上严格分离，但版本管理采用单仓库方式。这样更适合两人、两周的课程设计，能够减少联调等待，方便中期检查与最终验收统一展示，也便于通过一套提交记录呈现完整的软件工程过程。

本项目不拆分为`weather-frontend`和`weather-backend`两个独立仓库。

## 当前目录与未来职责

目前仅文档与 Canvas，课程设计/ 内有 00 项目总览、01 需求、02 系统设计、03 数据库设计、05 测试与验收、06 项目管理（含当前初始化计划 V2 与其他历史计划）、09 白板、AGENTS.md 及 docs/context/；根 versions/ 已有 V1 和 V2 阶段记录。

未来规划（本轮不创建工程）：

```text
仓库根/
├── frontend/      # 当前最新 Vue
├── backend/       # 当前最新 Spring Boot
├── database/      # 当前最新 DDL/初始化脚本
├── 课程设计/      # 唯一当前文档，沿用现有编号体系
└── versions/      # 阶段说明、截图、测试、Git 信息
```

不另建根 docs/ 重复体系。frontend/backend 物理分离，不使用混合 src/main/java 与 src/frontend；Git commit/tag 保存源码历史，versions/ 不复制完整源码或 node_modules/target。

## 目录职责与前后端边界

### `frontend/`

只负责Vue 3页面、Element Plus组件、ECharts图表、Axios请求、Vue Router路由、前端API调用和页面交互。前端不得直接访问数据库，也不得自行定义与后端不一致的接口字段。

### `backend/`

只负责Spring Boot应用、Controller、Service、Mapper、Entity、DTO/VO、参数校验、REST API和数据访问。Controller不直接访问Mapper，业务规则集中在Service。

### `database/`

负责MySQL DDL、初始化数据、演示数据和数据库实施说明。`schema.sql`是数据库结构变更的版本化依据，`data.sql`用于可复现的初始化或演示数据。

### `课程设计/` 与 `versions/`

课程设计/ 保存当前需求、系统设计、数据库设计、测试验收、上下文和 Canvas。versions/ 每阶段保存目标、功能、前后端与数据库状态、测试、截图、API/数据库变化摘要、真实 branch/commit/tag。未提交写“未提交”，无 tag 写“未创建”。


## Git分支策略

### 主分支

`main`是项目稳定主分支：

1. `main`应尽量始终保持可运行或至少处于可检查状态。
2. 不直接在`main`上进行大规模开发。
3. 新功能优先创建独立短期分支。
4. 功能完成并自测后，再通过Pull Request或两人共同确认合并到`main`。
5. 每次开始新任务前，优先同步最新`main`。

推荐开始方式：

```bash
git switch main
git pull
git switch -c feat/backend-forecast-query
```

开发完成后，仅暂存本任务涉及的具体文件：

```bash
git status
git add <本任务实际修改文件>
git diff --cached
git commit -m "feat(backend): add forecast query api"
```

以上命令仅为后续开发流程示例，本轮没有执行。

### 短期分支命名

| 前缀 | 用途 | 示例 |
| --- | --- | --- |
| `feat/` | 新增功能 | `feat/backend-city-crud`、`feat/backend-forecast-query`、`feat/frontend-query-page`、`feat/frontend-temperature-chart`、`feat/frontend-precip-chart`、`feat/frontend-data-management` |
| `fix/` | 修复问题 | `fix/forecast-query-params`、`fix/chart-refresh`、`fix/frontend-backend-integration` |
| `docs/` | 文档工作 | `docs/requirement-analysis`、`docs/database-design`、`docs/system-design`、`docs/midterm-review`、`docs/final-acceptance` |
| `test/` | 测试工作 | `test/backend-api`、`test/database-integrity`、`test/system-acceptance` |

一个功能分支只解决一个相对独立的问题，完成后及时合并并删除，不长期保留。

本项目不采用完整Git Flow，不设置长期`develop`、`release`、`hotfix`、`frontend`或`backend`分支。团队只有两人、开发周期约两周，复杂分支体系会增加不必要的同步和合并成本。

### 分支生命周期

```text
main
  ↓
创建短期功能分支
  ↓
开发与本地测试
  ↓
提交清晰的小粒度改动
  ↓
同步最新main并解决冲突
  ↓
最终检查
  ↓
合并main
  ↓
删除已完成的功能分支
```

### 推荐的第一批分支示例

成员A可按任务创建：

- `docs/database-design`
- `feat/backend-basic-crud`
- `feat/backend-forecast-query`
- `feat/frontend-temperature-chart`

成员B可按任务创建：

- `docs/system-design`
- `feat/frontend-basic-ui`
- `feat/frontend-data-management`
- `feat/frontend-precip-chart`

共同任务可使用：

- `fix/frontend-backend-integration`
- `test/final-acceptance`

这些只是分支命名示例，不表示分支已经存在，也不要求同时创建全部分支。实际分支应随具体任务创建。

## 两人协作方式

成员A主要负责数据库设计、后端查询、REST API和温度趋势图；成员B主要负责前端基础页面、基础数据管理、降水柱状图和页面联调。

主责划分不代表完全割裂。两人共同参与：

- 需求分析；
- 系统设计；
- 数据库评审；
- 前后端接口确认与联调；
- 测试；
- 中期检查；
- 最终验收。

任何数据库字段、接口参数或响应结构调整，都应先通知另一名成员并评估跨目录影响。

## 前后端接口协作

接口开发前至少确认：

- URL；
- HTTP Method；
- Request参数；
- Response JSON；
- 字段名称；
- 时间格式；
- 错误返回。

API 当前已冻结为 [V2.0](02-系统设计/04-接口设计.md)：16 个管理 CRUD，加 workbench、trend、comparison 三展示查询。管理筛选五参数可选、时间成对；展示查询按各契约必填项。前端不得自行假设字段；若实际实现冲突需报告并同步契约，不再把记录列表接口当作工作台逐时刻查询。

后端接口完成并合并`main`后，前端功能分支应及时同步：

```bash
git fetch origin
git merge origin/main
```

本课程设计统一使用`merge`同步，不在团队内混用`merge`和`rebase`。

## 数据库变更规则

数据库结构变更必须同步更新：

- `database/schema.sql`；
- `课程设计/03-数据库设计/`相关文档。

如果修改`forecast_record`字段，还必须同步检查：

- Entity；
- Mapper；
- Service；
- REST API；
- 前端字段映射；
- 数据字典；
- ER图与关系模型图。

禁止只修改本地MySQL而不提交`database/schema.sql`。初始化或演示数据调整应同步`database/data.sql`及数据说明。

## 提交规范

项目采用简化的Conventional Commits：

| 类型 | 用途 |
| --- | --- |
| `feat:` | 新增功能 |
| `fix:` | 修复问题 |
| `docs:` | 文档更新 |
| `test:` | 测试更新 |
| `refactor:` | 不改变功能的代码重构 |
| `chore:` | 构建、配置或其他维护工作 |

示例：

```text
docs: complete requirement analysis
docs: add database er design
feat(backend): add city management
feat(backend): add forecast query api
feat(frontend): add weather query page
feat(frontend): add temperature chart
feat(frontend): add precipitation chart
fix: resolve forecast query integration issue
test: add system acceptance cases
```

一次提交只完成一个清晰目标。应使用`feat(backend): add city query api`这类可理解的描述，避免`update project`、`finish everything`等无法说明范围的提交信息，也不要为了制造记录而人为拆分没有独立意义的提交。

## Git暂存检查

提交前必须执行：

```bash
git status
git add <本任务实际修改文件>
git diff --cached
```

优先按任务暂存具体路径，例如`backend/src/...`或`frontend/src/...`。避免无检查地执行`git add .`，尤其是在文档、IDE配置、本地数据库配置和业务文件同时变化时。

## 敏感信息与忽略规则

不得提交：

- MySQL真实密码；
- 本机绝对路径配置；
- `.env`中的真实密码；
- IDE缓存；
- `node_modules/`；
- `target/`；
- 日志；
- 临时文件。

密码和本地配置优先通过环境变量或被`.gitignore`忽略的`application-local.yml`等文件管理。仓库README、示例配置和提交记录中都不得出现真实密码。

## 中期检查与最终验收

中期检查时，GitHub应能够看到：

- 文档提交；
- 数据库设计提交；
- 后端提交；
- 前端提交；
- 两名成员各自的有效提交记录；
- 短期功能分支及合并过程。

这些记录用于体现真实的软件工程过程，不应通过拆分无意义提交伪造开发轨迹。最终验收前，`main`应包含经过确认的代码、数据库脚本、文档和演示材料，并完成一次端到端检查。

## 协作规范速览

| 项目 | 规则 |
| --- | --- |
| 仓库 | Monorepo |
| 工程目录 | `frontend/`、`backend/`、`database/`、`课程设计/`、`versions/` |
| 主分支 | `main` |
| 开发分支 | `feat/*`、`fix/*`、`docs/*`、`test/*` |
| 分支原则 | 短分支、单一任务、完成后及时合并和删除 |
| 提交原则 | 小提交、清晰目标、提交前检查暂存区 |
| 同步方式 | 统一使用`merge`，频繁同步`main` |
| 联调原则 | 先确认接口契约，后端完成后尽快合并 |
| 稳定性 | `main`尽量保持可运行或可检查 |

## V2 范围与下一阶段

山东地图 P0，ECharts Map + 静态 GeoJSON；16 市、ECMWF/NOAA、T2M/PRECIP。工作台批量加载、前端时间轴本地切换；趋势双图四统计；模型对比只做视觉展示；四类完整 CRUD。仍四表、3NF、无 Redis、无复杂 GIS。

V2 设计保持冻结，versions/v0.2-api-contract/ 保留历史内容；早期 00 文档体系计划及 06 中其他旧管理计划仅作历史安排参考，05-工程初始化计划已修订为当前 V2 计划。原课程日期不变，9/11 中检、9/18 软件验收、9/25 材料提交。

下一阶段仅 PROJECT-INIT-1：收到新任务后初始化 frontend/、backend/、database/，建立 versions/v0.3-project-init/。本轮没有开始编码、建库、下载 GeoJSON、安装依赖、commit、push 或 tag。
