# v0.13 — Management Frontend CRUD

Status：FE-MANAGEMENT-CRUD-1 COMPLETED

Gate：READY FOR SYSTEM FINAL REVIEW

## 目标与范围

完成 /management 四资源基础 CRUD 前端，沿用 [v0.12 后端基线](../v0.12-be-management-crud/README.md)。不增加后端 API、Schema、Seed、索引、分页、高级筛选、导入导出、权限、图表或动态 CRUD 框架。本阶段完成不等同全系统最终验收、生产就绪或零缺陷。

## 页面与实现

[DataManagement.vue](../../frontend/src/views/DataManagement.vue) 提供暗色页面框架和四个 Element Plus Tab；[四个独立资源面板](../../frontend/src/components/management/) 分别管理 City、Forecast Model、Weather Element、Forecast Record。默认 City，首次进入加载，返回可保留页面内数据；记录列表全量、id ASC，展示真实 VO 的名称、编码、时间、数值和单位。

[Management API](../../frontend/src/api/management.js) 复用既有 Axios，三个字典 GET 直接复用已有实现。使用以下 16 个接口，不改契约：

| 资源 | 列表 | 新增 | 修改 | 删除 |
|---|---|---|---|---|
| City | GET /api/cities | POST /api/cities | PUT /api/cities/{id} | DELETE /api/cities/{id} |
| Model | GET /api/forecast-models | POST /api/forecast-models | PUT /api/forecast-models/{id} | DELETE /api/forecast-models/{id} |
| Element | GET /api/weather-elements | POST /api/weather-elements | PUT /api/weather-elements/{id} | DELETE /api/weather-elements/{id} |
| Record | GET /api/forecast-records | POST /api/forecast-records | PUT /api/forecast-records/{id} | DELETE /api/forecast-records/{id} |

ElDialog 编辑复制 row 为独立 Draft；取消不修改原行，失败保留输入。ElMessageBox 确认删除，后端成功才刷新；冲突保留行。安全后端消息优先，状态区分 400/404/409/500/网络错误，不直接展示 Axios 对象。保存成功但后续 GET 失败单独提示，避免重复写入。

[局部状态工具](../../frontend/src/utils/managementState.js) 防止重复提交、旧响应覆盖及卸载后更新；字典写成功刷新自身，同时使记录元数据及对应选项失效，下次使用前读取。无 Pinia 或复杂缓存。

[校验工具](../../frontend/src/utils/management.js) 对齐 DTO 的必填、长度、坐标范围/六位小数、数值精度和安全数字 ID。记录用名称下拉提交数字 ID；日期为本地 yyyy-MM-dd HH:mm:ss，不转 UTC。value 最多两位小数，PRECIP 非负且 0.00 合法，T2M 可为负；不新增气象上下限。模型描述清空提交 null，空描述显示 --。

## 测试与构建（前轮实际执行）

- 原有 node --test 71 项保持不变；按授权新增 Vitest、Vue Test Utils、DOM 环境与最小配置，不迁移原测试。
- 最近 npm test：142 passed / 0 failed / 0 skipped。原有 71 + 新增 71；实际执行链为 Node 88 项 + 组件 54 项。
- 覆盖四资源交互、精确 DTO、Loading/Empty/Error/Retry、冲突保留、写成功但刷新失败、字典失效和竞态/卸载边界；按 TDD 实施。
- npm run build：PASS；共享包 >500 kB 警告保留。
- 本轮仅文档收口，未重新执行测试或构建；未发现最近验证后的相关源码变化。

## 真实 CRUD 浏览器证据（前轮实际执行）

- 四资源新增、编辑、删除通过；独立 Draft、取消不改原行、失败保留输入、删除冲突保留行。
- 描述清空提交 null 后重读为 --；记录下拉名称/数字 ID、本地时间正确，PRECIP 负值拦截与真实 0.00 保留；城市编码与记录联合唯一冲突通过。
- 字典写入后，已加载的记录 Tab 可重新读取新增要素。
- 临时要素 #8（TMP_FINAL_ELEMENT）由临时记录 #202 引用：删除、修改编码、修改单位的 UI 冲突均验证，另以相同目标独立 HTTP 复核均为 HTTP/code 409。原行保留，编码/单位草稿保留且未提前显示到列表。
- 1366×768 列表与弹窗检查通过。此前自动浏览器的宽屏截图/网络拦截限制如实保留为工具历史，不将未执行的工具操作改写为通过；本次缺项由下述用户证据补齐。

## 用户补充验收与受控模拟

下表结果均来自本轮用户最新明确确认，证据来源 USER_MANUAL_CONFIRMED；不是 Codex 本轮操作或重新验证。

| 项目 | 结果与证据边界 |
|---|---|
| GET Loading | 浏览器限速下可见，框架保留，放行后恢复真实列表 |
| GET Error / Retry | 指定列表请求被阻止后出现局部错误；解除阻止后页面内 Retry 恢复。属于受控网络失败，不宣称后端真实返回 HTTP 500 |
| 记录字典恢复 | 依赖字典失败提示及重试恢复通过 |
| City / Model / Element / Record Empty | 四资源 BROWSER_SIMULATED_EMPTY，通过浏览器空响应模拟验收，不是真实 MySQL 空表验证 |
| 1920×1080 | 四 Tab 有数据列表、记录内部滚动、新增/编辑弹窗、错误提示、操作按钮、删除确认框及无严重溢出/遮挡，人工确认通过 |
| 模拟撤销与 Console | 用户确认撤销模拟后真实列表恢复，正常状态 Console 无问题；受控故障请求日志与正常异常分开 |

## 数据清理与回归

前轮实际先删除临时记录 #202，再删除临时要素 #8，真实 GET 复核两 ID 均不存在；最近真实四列表为 16 / 2 / 2 / 192。这里只证明数量基线恢复和指定临时 ID 移除，不夸大为全部原有字段逐值校验。本轮没有数据库写入或重新执行 CRUD。

前轮有效浏览器回归：/weather 地图、Timeline、济南详情；/analysis 双图及四项统计；/comparison 双模型曲线，均通过。本轮沿用，不冒充重新测试。

## 本轮只读检查与文档收口

分支 feat/fe-management-crud，保留本阶段未提交实现。只同步 [CURRENT_CONTEXT](../../课程设计/docs/context/CURRENT_CONTEXT.md)、[RESUME](../../课程设计/docs/context/RESUME.md) 与本归档；核对 Git 差异、源码证据和文档链接/空白/冲突标记。没有业务、测试、依赖或历史归档修改。

Git writes = NONE。未创建 commit、tag 或执行 push；本归档不表示代码已经提交或合并，不复制源码、截图或大段日志。

## 已知非阻塞事项与下一阶段

- 共享包 >500 kB 构建警告，未优化。
- 课程设计/.DS_Store 仍被 Git 跟踪，独立仓库清理项，未移除。
- 要素单位引用冲突仍沿用现有编码冲突文案，未优化。
- 旧文档中工程未初始化、Comparison PRECIP 柱状表现等与已批准实现存在差异，留待系统一致性审查，不在本轮改写设计或历史。

下一建议：系统级最终回归与需求、文档、实现一致性审查。READY FOR SYSTEM FINAL REVIEW；须用户另行授权，本轮不自动开展。
