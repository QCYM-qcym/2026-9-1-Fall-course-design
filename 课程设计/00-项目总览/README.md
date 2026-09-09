# 《山东省气象预报数据可视化系统的设计与实现》

## 项目定位与当前阶段

两人、约两周课程设计。四页面、19 个业务 API 与数据库闭环已完成阶段验收，归档至 v0.13；系统最终验收状态见 [CURRENT_CONTEXT](../docs/context/CURRENT_CONTEXT.md)。数据为固定合成课程演示数据，不是实时气象服务。

## V2 核心范围

Weather Workbench（山东地图 P0、16 市、批量加载、本地时间轴）、Trend Analysis（双要素与四统计）、Model Comparison（ECMWF/NOAA 视觉对比）、Data Management（一个 Tabs 页面四类完整 CRUD）。

前端 Vue 3/Vite/JavaScript/Element Plus/ECharts/Axios/Vue Router；后端 Java 17/Spring Boot 2.7.18/MyBatis-Plus/MyBatis XML/Maven 3.9.16；MySQL 8.0.16+。仍四表，T2M/PRECIP，API 19 个。

## 导航与历史

- [项目导航](项目导航.md)：当前有效设计入口。
- [项目当前状态](项目当前状态.md)：实现范围与最终验证入口。
- [需求冻结 V2](../01-立项与需求/09-需求冻结确认.md)
- [API V2](../02-系统设计/04-接口设计.md)

早期文档体系计划、06 中其他旧管理计划和 v0.2 API 归档保留历史身份，旧范围不作为当前规范；[工程初始化计划 V2](../06-项目管理/05-工程初始化计划.md) 已执行。根 frontend/、backend/、database/ 保存当前实现，课程设计/ 保存唯一文档，versions/ 保存阶段档案，不复制代码。

当前进行 SYSTEM-FINALIZE-1，不自动进入新阶段；本地开发配置不等于生产部署配置。中检 9/11、软件验收 9/18、材料提交 9/25 保持原日期。
