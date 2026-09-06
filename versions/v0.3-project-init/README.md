# v0.3 Project Init

## 版本目标

完成前端、后端、数据库工程骨架和当前技术基线文档同步，不实现业务功能。

## 环境与版本

- OS：Windows 11 amd64
- Java：17.0.11
- javac：17.0.11
- Maven：Apache Maven 3.9.16
- Spring Boot：2.7.18
- Node.js：v24.18.0
- npm：11.16.0
- Vue：3.5.30
- Vite：7.3.1
- Vue Router：4.6.4
- Element Plus：2.13.3
- ECharts：6.0.0
- Axios：1.13.6
- MyBatis-Plus：3.5.7
- MySQL Connector/J：8.0.33

## Frontend

已完成：

- Vue 3 + Vite + JavaScript 工程骨架。
- `/` 重定向到 `/weather`。
- `/weather`、`/analysis`、`/comparison`、`/management` 四个路由和最小深色 App Shell。
- Element Plus 已接入，ECharts 使用模块化 CanvasRenderer 导入，Axios 实例使用 `/api` 和 10 秒超时。
- Vite `/api` 代理默认指向 `http://localhost:8080`，可由 `VITE_API_PROXY_TARGET` 覆盖。

未实现：山东地图、GeoJSON、时间轴、查询表单、图表配置、缓存、Trend、Comparison 和 CRUD。

## Backend

已完成：


- `com.shandong.weather.WeatherApplication` 主类及 Java 17 编译配置。
- Spring Boot Web、Validation、MyBatis-Plus、MySQL Connector/J 和 Spring Boot Test 依赖。
- `ApiResponse<T>`、`BusinessException`、`GlobalExceptionHandler` 最小公共骨架。
- 工程健康检查 `GET /api/health`，返回 `code=200`、`message=success`、`data=ok`；该接口不计入 API V2 的 19 个业务接口。
- 数据源通过 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD` 外部配置，未连接业务表。

未实现：四表 Entity/Mapper/Service/CRUD、Workbench、Trend、Comparison 和其他业务 API。

## Database

已创建：`database/schema.sql`、`database/data.sql`、`database/README.md`。

当前只有脚本说明骨架；没有执行 DDL、建库或导入数据，也没有生成 3584 条预报记录。正式四表结构、约束、索引和基础字典数据属于 DB-INIT-1。

## Verification

实际执行结果：

- `mvn test`：BUILD SUCCESS；Tests run 1，Failures 0，Errors 0，Skipped 0。
- `mvn package`：BUILD SUCCESS。
- `javap -verbose backend/target/classes/com/shandong/weather/WeatherApplication.class`：major version 61。
- 无 MySQL 服务时启动打包 jar，`GET /api/health` 返回 `{"code":200,"message":"success","data":"ok"}`；验证后已终止服务。
- `npm run build`：成功，Vite 7.3.1 完成生产构建。
- Vite 开发服务器对 `/`、`/weather`、`/analysis`、`/comparison`、`/management` 均返回 HTTP 200；验证后已终止服务。

## 文档技术基线同步

当前有效文档统一为 Java 17、Spring Boot 2.7.18、Maven 3.9.16、Java compiler 17、class major version 61。`versions/v0.2.1-system-design-v2/` 和早期文档体系计划中的 Java 8 属于历史阶段记录，未篡改。

## Git

- Branch：`feat/project-init`
- Commit：未提交
- Tag：未创建

## 已知问题

- 当前 PowerShell 会话中 `mvn` 命令未刷新到 PATH，验证使用用户确认的 Maven 3.9.16 安装路径；未修改全局 PATH 或 MAVEN_HOME。
- npm 构建提示主 bundle 较大，但不影响初始化构建；后续功能开发时再按需拆分。

## 下一版本

DB-INIT-1：审查并实施四表 DDL、完整约束、最小索引、16 市及两模型两要素基础字典和少量可验证演示数据。
