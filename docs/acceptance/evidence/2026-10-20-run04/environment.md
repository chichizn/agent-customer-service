# run04 环境记录

- 时间：2026-10-20 00:00（Asia/Shanghai）。
- MySQL Server 8.0.42，独立数据目录 `backend/target/acceptance-mysql/data`，仅绑定 `127.0.0.1:13307`；验收库为 `agent_customer_service_acceptance_20261019`。
- 隔离库内加载集成演示种子，包含2店、5顾客、12订单和7物流；共22张应用表。
- 本机已有 MySQL80/3306 未修改。隔离实例使用独立数据目录和端口，不复用或清空已有 MySQL 数据。
- 后端 API 为本机 `127.0.0.1:10002`；LLM 与 Redis 关闭，锁提供者为 local。
- [commerce-readonly.json](commerce-readonly.json) 的34项均只发身份、订单、物流和工单查询 POST；不执行会话、重置、建单或模型调用。
- 报告 revision 是当时 Git HEAD；工作区有本地 Redis/锁启动修复，不能把 HEAD 字段当作干净源码证明。
