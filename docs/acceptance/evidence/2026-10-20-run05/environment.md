# run05 工作台 HTTP 环境与写入范围

- 时间：2026-10-20 00:11 Asia/Shanghai。
- 后端：本机 `127.0.0.1:10002`；数据库：隔离 MySQL `127.0.0.1:13307/agent_customer_service_acceptance_20261019`；MySQL 8.0.42。
- LLM 关闭，Redis 关闭；规则查询可用，售后写入需求转 `WAITING_HUMAN`。
- 运行 `node docs/acceptance/scripts/workbench-smoke.mjs --allow-write --database agent_customer_service_acceptance_20261019 <report.json>`。脚本仅允许回环 HTTP，且目标 schema 参数固定；调用前仍需确认 API 的实际数据源。
- 脚本创建并保留5个会话与消息，未执行 demo reset、未创建工单、未调用模型。随机会话 ID、消息编号和逐项检查记录见 [workbench-smoke.json](workbench-smoke.json)。数据库复核为5个本轮会话、0张工单。
- 运行时工作区有未提交代码修改，JSON 中 revision 记录基线 HEAD `09941547...`。首次 HTTP 进程早于最后锁改动；随后确认进程归属并停止本轮 API，用当前源码重新启动（Java PID 32632，仅监听127.0.0.1:10002），继续连接同一隔离库。
- 使用只读 `workbench-restart-check.mjs` 在重启前 capture、重启后 verify：[五个会话完整快照一致](restart-after.json)。比较对象包含消息、执行记录、人工回复、接待模式和已选订单；工单均为0。
- 00:25 重启后再次通过[34项 commerce 查询](commerce-after-restart.json)及[6组工作台冒烟](workbench-after-restart.json)。后一次冒烟另建并保留5个会话；两次均未重置、未建工单、未调用模型。后一次报告显式记录 `workspaceDirty=true`。
- 当前源码的最后49项全量后端测试于00:25:42通过。完整源码版本边界见[当日验收记录](../../RESULTS-2026-10-20.md)。
