# v0.2.1 System Design V2

## 版本目标

冻结 V2 前端、后端、数据库和 API，为下一阶段工程初始化提供唯一当前设计。

## 核心变化

- Windy 交互思想参考的深色气象工作台成为首页；不复制其资产或代码。
- 山东地图成为 P0，ECharts Map + GeoJSON，初始化覆盖 16 城市。
- 四页面：Workbench、Trend、Comparison、Data Management。
- 四类完整 CRUD；Trend 双要素与四统计；Comparison 双模型视觉对比。
- Weather Query APIs：workbench / trend / comparison，批量加载与前端本地时间轴、缓存失效。
- 取消 V1 人为数值范围，保留精度与 PRECIP 非负；缺测不补零。
- 中检最低能力与最终完整验收分开。

## 前端 / 后端状态

Frontend：未初始化。Backend：未初始化。全部仍为设计状态，没有实际页面、Java、Vue 或已实现接口。

前端 Vue 3/Vite/JavaScript/Element Plus/ECharts/Axios/Vue Router；后端 Java 8/Spring Boot 2.7.18/MyBatis-Plus/MyBatis XML/Maven。五 Controller、五 Service、四 Mapper；核心 SQL 放 ForecastRecordMapper.xml。

## 数据库

Database：未初始化。仍 city、forecast_model、weather_element、forecast_record 四表，3NF 和业务唯一键不变。16×2×2×56 ≈ 3584 条仅规划，未生成。预报记录为业务唯一、工作台联合、要素外键三项二级索引，另有主键；未运行 EXPLAIN。

## API

19 个核心 REST API：12 字典 CRUD + 4 记录 CRUD + 3 气象查询。唯一当前契约见下方链接，本目录不复制整份 API 或源码。v0.2-api-contract/ 保留 V1 原文，未覆盖。

## 测试结果

本阶段仅文档与 Canvas 静态检查；不代表运行测试、性能测试或功能验收通过。正式 JUnit/Spring Boot Test/Postman/Apifox 与 GUI 验收均未运行，原因是工程未初始化。验收清单为后续执行方案。

2026-09-06 静态检查记录：19 个方法/路径无重复，7 个 JSON 示例可解析；10 张 Canvas 共 129 个节点、95 条连线，ID/引用有效、无非分组节点重叠；正文按20px字体测量无溢出，并复核布局预览（不是 Obsidian 原生运行测试）。276 处文档/Canvas 文件引用有效，代码中的链接语法示例不计入。DDL 四表字段与本轮开始前一致，仅替换预定工作台索引。git diff --check 通过，仅有 LF/CRLF 转换提醒。文件哈希核对确认 V1 归档和无关既有改动未变，本轮仓库仅修改36个已有文档/Canvas并新增3个Markdown文件。

## Git

Branch：main

Commit：未提交

Tag：未创建

不得把本轮未提交资料写成已发布版本。真实源码历史由 Git commit/tag 保存，versions/ 只记录阶段说明、测试、截图与变更摘要。

## 已知事项

base package、具体依赖版本、本机环境、GeoJSON 来源许可与映射、正式演示数据留待后续任务；不构成新增业务范围。00 早期计划与06历史管理计划的旧功能口径不作为当前规范。

## 当前设计入口

- [需求冻结 V2](../../课程设计/01-立项与需求/09-需求冻结确认.md)
- [前后端架构](../../课程设计/02-系统设计/01-总体架构设计.md)
- [前端 V2](../../课程设计/02-系统设计/02-功能模块设计.md)
- [唯一 API V2](../../课程设计/02-系统设计/04-接口设计.md)
- [数据库 SQL 设计](../../课程设计/03-数据库设计/08-SQL设计说明.md)
- [中期检查](../../课程设计/05-测试与验收/01-中期检查方案.md)
- [最终验收](../../课程设计/05-测试与验收/02-最终验收方案.md)
- [当前上下文](../../课程设计/docs/context/CURRENT_CONTEXT.md)

## 下一版本

仅 PROJECT-INIT-1：在新任务授权后初始化 frontend/、backend/、database/，建立 versions/v0.3-project-init/。本轮到 V2 设计冻结为止，不自动初始化。
