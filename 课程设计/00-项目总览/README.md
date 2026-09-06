# 《山东省气象预报数据可视化系统的设计与实现》

## 项目定位与当前阶段

两人、约两周课程设计。当前 System Design V2 Freeze，UI、后端、数据库范围与 API 已统一冻结；尚未创建前后端工程、数据库实例或业务实现。

## V2 核心范围

Weather Workbench（山东地图 P0、16 市、批量加载、本地时间轴）、Trend Analysis（双要素与四统计）、Model Comparison（ECMWF/NOAA 视觉对比）、Data Management（一个 Tabs 页面四类完整 CRUD）。

前端 Vue 3/Vite/JavaScript/Element Plus/ECharts/Axios/Vue Router；后端 Java 17/Spring Boot 2.7.18/MyBatis-Plus/MyBatis XML/Maven 3.9.16；MySQL 8.0.16+。仍四表，T2M/PRECIP，API 19 个。

## 导航与历史

- [项目导航](项目导航.md)：当前有效设计入口。
- [项目当前状态](项目当前状态.md)：已设计、未实现和下一任务。
- [需求冻结 V2](../01-立项与需求/09-需求冻结确认.md)
- [API V2](../02-系统设计/04-接口设计.md)

早期文档体系计划、06 中其他旧管理计划和 v0.2 API 归档保留历史身份，旧范围不作为当前规范；[工程初始化计划 V2](../06-项目管理/05-工程初始化计划.md) 是当前初始化依据，尚未执行。未来根 frontend/、backend/、database/ 保存当前实现，课程设计/ 保存唯一文档，versions/ 保存阶段档案，不复制代码。

下一阶段仅 PROJECT-INIT-1；本轮不自动初始化。中检 9/11、软件验收 9/18、材料提交 9/25 保持原日期。
