# DEMO-PORTABLE-1 Implementation Plan

**Goal:** 将已验收 v1.0 制作为 Windows x64 离线演示 ZIP，不改业务契约。
**Architecture:** Vue dist 在 Maven portable profile 构建时进入 JAR；仅四条页面路径回退 index。包内 Java/MySQL、独立 var 数据与私有配置；固定回环端口 18080/13306，无服务安装。
**Spec:** 用户本轮已批准的 DEMO-PORTABLE-1 请求。
**Constraints:** 不连接开发库3306，不改Schema/Seed、依赖或历史归档；不写Git；目标电脑不用全局Java/Node/Maven；生成物仅 .portable-build/。

- [x] 路由与停止钩子：先 MockMvc/单元失败测试，后实现严格页面白名单与仅 portable 启用的文件请求优雅关闭；验证 API/缺失静态资源不回退。
- [x] 运行脚本与测试：独立目录首次初始化、凭据、最小 CRUD 权限、端口与进程归属核验、真实字典就绪、停止/重启保留数据。
- [x] 下载与构建：锁定官方 Temurin 17.0.11+9 与 MySQL 8.0.46 ZIP，校验官方值；自动构建 dist + Maven profile，保留许可证及对应源码。
- [x] 本机发布验收：最终 ZIP 解压后实际运行，四页/API/临时 CRUD/正常停止重启通过；外链不可用仅完成下述受控模拟，未冒充物理断网。
- [x] 交付：从未运行的干净模板压缩，无 var、运行日志或生成密码；保留已有工作区改动。

## 2026-09-09 实际结果

状态：PACKAGE_READY / TARGET_MACHINE_VALIDATION_PENDING。
本机演示包功能验证通过，目标笔记本待验证；物理断网测试未执行。

- 最终包：`.portable-build/releases/20260909-151505-4d23389a/shandong-weather-demo-v1.0-win-x64.zip`，909,756,719 bytes。
- SHA-256：`feecb6a94d1196b072479adf3e25e4d510750feb972941873198666ff6352970`，同目录附 `.sha256`。
- 本轮实际执行：前端 146 tests PASS、build PASS；portable Maven clean package 284 项非数据库测试 PASS（新路由/停止测试 11 项包含在内），repackage PASS。没有冒充重跑开发库完整测试；开发库未连接。
- 脚本最终 94 项断言 PASS，包括 ANSI 配置编码、登录文件隔离、重复 ACL 设置、退出中 PID 信息缺失与真实身份冲突的区分、初始化/停止互斥及初始化退出未确认标记；相关修复先失败再实现。
- 最终 ZIP 解压到 `最终离线包 test 7` 中文/空格目录：首次初始化、真实字典就绪、启动 CMD 自动打开管理页、停止 CMD、再次启动与停止均 PASS。此前 test 6 实测重复启动因端口占用拒绝，原实例保持可用；该端口保护实现未改变。
- 最终 test 7 实际在初始化完成标记出现前发出 Stop：Stop 等待共享生命周期锁，待启动段结束后关闭应用和数据库；启动监督进程退出、无剩余监听、无残留 run-state，PASS。独立只读审查指出的启动/停止竞态已修复；初始化退出无法确认时明确拒绝宣称停止成功。超时标记分支为 fixture 验证，未人为挂起真实初始化进程。
- 四类 API 数量 16/2/2/192；Workbench 3 times / 48 records；Trend 3+3 点及 21.10/19.00/20.10/0.60；Comparison T2M/PRECIP 两模型实际值均 PASS。
- 最终 test 7 临时模型 `TMP_PORTABLE_CHECK` ID=3：POST、PUT、GET、正常停止/重启后保留、DELETE 均 PASS；最终列表中 ID=3 不存在，数量恢复 16/2/2/192。调试副本 test 5、test 6 的同名 ID=3 也已清理。未创建临时预报记录，未编辑 Seed；数量验收不等于逐字段全库比对。
- 实际浏览器访问及刷新 `/weather`、`/analysis`、`/comparison`、`/management` 正常，地图 16/16 显示真实数据。浏览器常规 error/warn 捕获为空；浏览器验收使用 test 5 中与最终包字节一致的 JAR，后续仅改启停脚本与 README。最终包 HTTP 再验不存在的 API、静态文件、未知页面均 404，不回退成首页。
- `BROWSER_CSP_EXTERNAL_BLOCK_SIMULATION`：独立 GET-only 临时代理转发同一实际发布 JAR，仅追加限制外链的 CSP；探针确认外链被阻止，四页地图/图表/真实列表仍正常。该代理不随包分发，已停止；此证据不是关闭网卡后的物理断网验收。
- 实际核对应用账号仅 `shandong_weather.*` 的 SELECT/INSERT/UPDATE/DELETE；MySQL 与 Java 仅监听 127.0.0.1:13306/18080，无其他该实例监听。结束后两端口及临时代理 18081 均释放；未注册服务、未改变全局设置。
- 最终 ZIP 共 847 项，原 SQL、JAR、三个运行脚本及 README 的 SHA-256 与当前源文件一致，ZIP 校验旁文件一致；无 var、运行密码、日志或个人配置。许可证目录中保留少量含 `node_modules` 字样的嵌套 LICENSE 路径，仅许可证文本，没有依赖代码目录。

## 必要修复与已知限制

- 打包通过 Maven `portable` profile 纳入 Vue dist；新增四路由白名单及仅显式配置时启用的本地停止文件钩子，没有新增业务 API。
- Windows 实测修复：MySQL INI 使用系统 ANSI 编码并严格检查往返；`--no-monitor` 避免 MySQL 重启监控器截断多字节路径；不设置非法空 admin-address；客户端使用相对配置路径与私有 MYSQL_TEST_LOGIN_FILE；DACL-only 权限设置支持重复启动；退出过程中字段暂缺只等待、不误杀；共享生命周期锁及持久退出未确认标记避免停止过早报成功。
- 中文目录在本机 CP936 验证通过；系统 ANSI 无法表示的路径会明确拒绝，应改用可表示的短目录名。目标系统仍需 Windows x64、PowerShell 5.1、浏览器、VC++ 2015–2022 x64 运行库及可写空间。
- 共享 JS 包体积警告、MySQL 自签名 CA 提示及应用关闭时 Tomcat 的 Hikari 线程提示为非阻塞：实际 Hikari 已关闭且进程/监听均退出。
- 启动完成后进程意外退出仍主要通过包内日志排障，启动器不提供故障重启或持续监控功能；独立审查将退出码提示不足列为非阻塞项。
- 数据为合成课程演示数据，不是实时气象服务；本地回环配置不是生产部署配置。目标笔记本运行与物理断网仍待实测。
- 失败初始化目录 test 1～4 保留在被忽略的验证目录，不删除重试、不随 ZIP 分发。最终包只指向上述版本，不分发早期调试 ZIP。

## 重复构建

在已有项目构建环境的仓库根目录执行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/portable/Build-Portable.ps1`。只影响该 PowerShell 进程，不修改全局策略；组织禁止脚本时应遵守管理员策略。
脚本使用锁文件校验官方归档并生成唯一发布目录；`-Offline` 要求四个上游归档已缓存，不代表 Maven/npm 依赖自动离线可用。`-PackageOnly` 仅用于已有已验证合并 JAR 后的运行脚本/许可再打包；业务或前端变化后必须重新完整构建。
分发原始干净 ZIP，不能压缩验收运行目录。Git writes = NONE；v1.0 历史归档未重写。
