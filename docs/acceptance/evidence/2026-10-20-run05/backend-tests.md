# run05 后端测试

- 时间：首次2026-10-20 00:08；最后一次2026-10-20 00:25:42 Asia/Shanghai。
- 环境：JDK 17.0.12、Maven 3.9.9、离线依赖。
- 命令：`mvn -o -B test`（工作目录 `backend`）。
- 结果：49 tests，0 failures，0 errors，0 skipped。
- 相比 run03 的37项，新增6项 `LocalLockManagerTest` 与6项 `DistributeLockAspectTest`。覆盖同 key 并发互斥、重入清理、默认等待、超时、中断恢复、不同 key 回收、SpEL 缺省/空值、未配置 provider 失败关闭、Redisson 获取释放，以及业务异常原样传播。
- 完整应用 H2 HTTP 测试也通过：Redis 关闭时应用可启动，且本地锁切面可以执行；H2 测试不作为真实 MySQL 业务验收。
- 最后全量执行耗时18.747秒，仍为49 tests、0 failures、0 errors、0 skipped。工作区日志写在被忽略的 `backend/target/acceptance-tests-final.log`；首次日志为 `backend/target/acceptance-tests.log`。没有将临时输出或本机配置加入证据。
