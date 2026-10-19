# run06 演示业务时间验证

执行于 2026-10-20（Asia/Shanghai）。HEAD 为 `e2559503689f3207f2279d85253264379154e4a4`，工作区包含本次未提交的日期改动。JSON 中的 revision 为运行基线，不表示当时工作区干净。

| 检查 | 结果 |
|---|---|
| `backend`：`mvn -o -B test` | 60 tests，0 failures/errors/skipped；00:41:19完成，24.285秒 |
| 时钟和知识新增回归 | 4项时钟单测、6项有效期/范围/旧元数据单测、1项实际JPA旧数据补齐及过期过滤集成测试；包含在上述60项中 |
| 前端工作台/commerce适配 | 14 tests，0失败，15.909秒 |
| `vue-tsc --noEmit` 与 `vite build` | 退出0，2951模块，Vite构建13.04秒；双端提示尚未做浏览器视觉验收 |
| C离线检查器 | 26 tests，0失败 |
| 数据与知识一致性 | PASS：2店、5顾客、3商品、12订单、7物流、10知识 |
| [旧知识升级](knowledge-migration.json) | 既有库升级前10条知识且无新增列；当前源码启动后补列，打开会话后10条均有类型、2条活动有完整日期；工单仍为0 |
| [未来基准过期检查](demo-time-expired-retry.json) | 固定2026-11-01 12:00+08，两店活动均不出现在引用或回复；消息仍记录10月20日真实时间 |
| [默认基准有效检查](demo-time-default.json) | 恢复2026-10-18 12:00+08，两店分别只命中本店活动；真实操作时间不变 |
| [commerce查询](commerce-readonly.json) | 34项只读HTTP检查PASS |
| [工作台冒烟](workbench-smoke.json) | 6组HTTP检查PASS；本轮新建5个会话 |

环境为 JDK17.0.12、Maven3.9.9、Node24.19.0、MySQL8.0.42。前端使用已安装依赖的直接 Node 入口，分别执行 `node node_modules/tsx/dist/cli.mjs --test scripts/workbench.test.ts scripts/commerceAdapter.test.ts`、`node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`、`node node_modules/vite/bin/vite.js build`。后端完整测试输出保留在忽略目录 `backend/target/demo-time-tests.log`。

HTTP只连接本任务的 `127.0.0.1:10002` 与独立MySQL13307，库为 `agent_customer_service_acceptance_20261019`，Redis关闭、锁为local、LLM关闭。当前进程使用 `acceptance` profile与显式环境变量，不加载私密local配置。没有调用load/reset/restore，没有修改原MySQL80/3306。初次过期探针创建1个会话，修正后过期/默认各2个，工作台冒烟另建5个，本轮共10个会话保留供回查。

[第一次探针](demo-time-expired.json) 的时间断言FAIL也保留：新脚本错误地给已经带`+08:00`的消息时间再次追加偏移，导致解析失败。修正为直接解析接口时间并校验显式偏移后，以新文件复跑通过；第一次记录不覆盖，不作为业务AC失败。

复现时间检查时，先核实API实际连接上述独立验收库，在后端终端设置 `DEMO_TIME_MODE=fixed` 和所需 `DEMO_REFERENCE_TIME` 后重启，再在仓库根运行以下命令（会新建会话与消息；每次选择新的输出文件名，保留原记录）：

```powershell
node docs/acceptance/scripts/demo-time-smoke.mjs --allow-write --database agent_customer_service_acceptance_20261019 '2026-10-18T12:00:00+08:00' docs/acceptance/evidence/demo-time-new-run.json
```

活动起止两端包含、非演示知识按真实时间、未知日期活动被排除等边界由自动化回归覆盖；HTTP本轮覆盖默认和过期两种固定基准，未实测system模式浏览器。不会据此证明自动退货资格判断、真实模型生成、聊天建单审批、含工单完整重启恢复或双端浏览器验收。完整AC-01～AC-22仍为0 PASS、0 FAIL、22 BLOCKED。
