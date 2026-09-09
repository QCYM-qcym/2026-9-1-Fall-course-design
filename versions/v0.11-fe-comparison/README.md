# v0.11 FE Comparison

FE-COMPARISON-1 COMPLETED · 2026-09-09

## 目标与实际文件

完成 /comparison 真实后端双模型视觉对比，保持既有路由。
修改 frontend/src/views/ModelComparison.vue、frontend/src/api/weather.js；trendAnalysis.js 仅导出既有时间校验函数。
新增 frontend/src/components/comparison/ComparisonChart.vue、frontend/src/utils/comparisonAnalysis.js、comparisonState.js 及对应两个测试文件。
JDBC 修复文件为 backend/src/main/resources/application.yml；本次收口仅修改两个 context 文件并创建本归档，不复制源码。

## API 与默认值

复用 Axios：GET /api/cities、GET /api/weather-elements、GET /api/weather/comparison；查询参数仅 cityId、elementId、startTime、endTime。
按编码定位 JINAN、T2M，不硬编码 ID；范围复用 2026-09-07 08:00:00～14:00:00。字典完成后自动查询一次，之后仅点击查询才请求。

## 图表与状态

暗色筛选栏、轻量 ECMWF/NOAA 摘要、主图；T2M 和 PRECIP 均为双折线，遵循本轮明确批准方案，不采用旧文档 PRECIP 分组柱状图。
两模型 forecastTime 时间并集 ASC 对齐；缺测映射 null 并断线，真实 0.00 保留；℃/mm 随响应切换。
Loading、Empty、Error、Retry 保留筛选、requestVersion Race Protection、resize/dispose；无 Cache，无 metrics、RMSE/MAE/Bias/Accuracy 或评分。

## Tests / Build

实现轮 TDD：先验证新增测试失败，再实现；原有 45 + 新增 26 = 71 passed / 0 failed / 0 skipped。
npm run build PASS；共享包 >500kB warning 为 NON-BLOCKING。独立只读审查无需修复项。
以上为此前真实执行结果；本次文档收口未重跑。后端 193 tests/package PASS 为前置 BE-COMPARISON 基线，不是 JDBC 修复后的新测试结果。

## Runtime / Regression

证据来源：用户本次提供的真实浏览器验收确认，不冒充 Codex 收口轮重新测试。
济南 2026-09-07 08:00、11:00、14:00：

| Element | ECMWF | NOAA |
|---|---|---|
| T2M（℃） | 19.00 / 20.20 / 21.10 | 18.40 / 19.60 / 20.50 |
| PRECIP（mm） | 0.00 / 0.40 / 0.20 | 0.10 / 0.60 / 0.30 |

Default T2M、PRECIP、City Switch、Empty、Loading、Error、Retry、Race 全部 PASS。
1366×768、1920×1080、Console、/weather Regression、/analysis Regression 全部 PASS。
数据是课程合成演示数据，不代表真实模型预测或准确率。

## JDBC Runtime 修复

DB-CONNECTION-CONFIG-FIX COMPLETED。
错误：Public Key Retrieval is not allowed。

原 JDBC URL：

```text
jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:shandong_weather}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
```

新 JDBC URL：

```text
jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:shandong_weather}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

仅追加 allowPublicKeyRetrieval=true；useSSL=false 及 DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD 环境变量机制不变，无密码入库，未改账号、Schema/Seed。此为本机开发连接配置，不作为远程生产安全配置建议。
用户确认修复后 GET /api/cities、/api/forecast-models、/api/weather-elements 均返回 200，/weather、/analysis、/comparison 恢复。
此前 Codex 执行 mvn test、mvn package 均因 Maven Central 访问被拒、父 POM 无法解析而未进入测试；不得记为测试通过。本次未重跑 Maven 或 HTTP，不声明服务持续在线。

## 已知非阻塞项与下一阶段

共享包体积提示、既有被跟踪的课程设计/.DS_Store 属于非阻塞项，本轮不清理。
/management 仍是占位页；READY FOR MANAGEMENT-BACKEND-AUDIT，须先满足审计独立 Git Gate，不直接开始 CRUD 实现。
本次收口开始分支 audit/management-backend，工作区 clean；未创建或声称本版本 commit/tag/push。Git writes = NONE，后续由用户通过 GitHub Desktop 操作。
