# D 成员交付说明

日期：2026-10-18。分支：`codex/commerce-d`。范围对应需求 §11：独立电商业务后端，供 A 的 Agent 和 B 的工作台调用，使用独立分支供评审与集成。

## 已完成

- `backend/src/main/java/com/anjing/module/commerce/`：商家、店铺、顾客、订单及明细、物流及节点、服务工单、处理历史、消息提交回执；新增业务 ID 均为字符串。
- 强制商家/店铺/顾客归属校验；准确单号和商品线索查单，多笔显式要求选择；按时间排列物流，未发货/取消/缺记录有明确说明。
- 两类本地工单；创建前参数及确认校验；数据库事务和悲观锁保证消息重试、未结同类诉求及并发提交去重；每次复用也有消息回执。
- 按工单号、订单或会话查询，跨会话复用仍可查；商家状态迁移、旧状态冲突检查及处理记录；终态不能重开。
- 明确标记的演示身份、12 笔订单、7 份物流记录；补齐加载、重复恢复、非演示数据保留和冲突回滚。
- 8 个可直接调用的 HTTP 接口、Java 服务、AI 写入 guard 接入点和模块专属错误返回。

接口请求/返回、状态、错误及 A 的调用约定见 [commerce-contract.md](commerce-contract.md)。可逐项执行的请求见 [commerce-api.http](../backend/commerce-api.http)。C 的数据模板及可加载种子为 [demo-data.json](../backend/src/main/resources/commerce/demo-data.json)。

## 验证结果

实际执行：Microsoft OpenJDK 17.0.20.1、Maven 3.9.9，完整后端 `mvn test` 成功；18 项测试，0 失败、0 错误、0 跳过。持久化测试使用 H2 2.3.232，仅在 test scope 引入，不改变运行时 MySQL 配置。已有代码的 Lombok/Redis 弃用警告没有阻止构建。

| 测试类 | 数量 | 已验证内容 |
|---|---:|---|
| CommercePersistenceTest | 14 | 演示数量与归属、模糊查单及条件交集、跨店/同店跨顾客隔离、物流节点顺序与缺失提示、确认及 AI guard 缺失时禁止写入、持久查询与历史、消息重试/不同会话复用/终态后重试、内容变更冲突及不同类型、状态迁移及终态约束、8 路并发建单、6 路并发状态更新、恢复保留非演示工单、混合数据及编号冲突回滚、真实数据库约束失败时工单/历史/回执全部回滚 |
| CommerceControllerTest | 4 | 响应包装及枚举、未确认/缺身份/非法参数不进入业务服务、404/409/数据库失败503不返回工单成功、恢复前显式确认 |

数据库故障测试主动添加必然失败的约束，日志中的 `FAIL_SUBMISSION` 错误是预期验证。HTTP 故障测试也主动模拟数据库离线。

其他成员已配置 Java17/Maven 时，在 `backend` 执行：

```powershell
mvn test
```

本机 PATH 原先没有 Java/Maven，已在忽略的 `backend/target/devtools/` 放入便携工具及依赖缓存，未修改系统环境。当前本机可这样复现：

```powershell
Set-Location C:\Users\ETBai\agent-customer-service\backend
$env:JAVA_HOME = (Resolve-Path 'target/devtools/jdk-17.0.20.1+1').Path
& './target/devtools/apache-maven-3.9.9/bin/mvn.cmd' -o -B '-Dmaven.repo.local=target/devtools/repository' test
```

`target` 是本地生成目录，清理后需自行准备 Java/Maven。测试报告在 `backend/target/surefire-reports/`。

## 接入安排

| 成员 | 后续接入操作 |
|---|---|
| A | 从会话绑定构造 scope；调用 getOrder/getLogistics/getServiceTicket；持久化已选订单和待确认诉求；实现 AgentTicketGuard 并调用 createAgentTicket；让接管操作竞争同一会话锁；接入真实执行记录及统一演示重置 |
| B | 按契约接订单/物流/工单卡片，NEED_SELECTION 时展示选择，创建后保留真实工单编号；提交 expectedStatus 更新工单并刷新处理历史；顾客侧使用 A 的会话接口 |
| C | 用给定身份执行淘宝多单物流、抖店退货与追问、异常工单及人工更新故事；准备知识与规则的店铺差异数据；验证 MySQL、重启持久化和系统统一重置 |

AI 写入缺少 guard 时会明确失败，禁止跳过接待模式校验。guard 的会话锁、身份/确认核验规则已在接口契约写明。独立商家 HTTP 创建入口供直接演示，不作为顾客聊天写入入口。

## 尚待整体集成验收

此次验证覆盖 commerce 服务、H2 事务及 HTTP 控制器；尚未在本机启动完整 MySQL/Redis/LLM 应用，也未执行浏览器双端业务验收或真实 MySQL 并发测试。A 的 AI 接管竞态、会话确认真实性、执行记录、知识范围、前端轮询与系统重置属于接入后的整体验收，不能据上述测试宣称 AC-01～AC-22 已全部完成。

本模块直接 HTTP 接口沿用现有项目的单商家演示环境，显式 scope 校验不替代生产认证。工单只记录本地诉求，始终返回 `NOT_SUBMITTED_TO_PLATFORM`，不执行平台退款。
