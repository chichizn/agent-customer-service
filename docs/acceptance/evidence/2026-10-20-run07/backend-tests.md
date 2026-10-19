# run07 后端测试记录

2026-10-20 00:59:36 +08:00完成。在 `backend` 目录运行 `mvn -o -B test`，Maven 3.9.9、JDK 17.0.12，耗时24.480秒，退出0，BUILD SUCCESS。

| 套件 | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| DistributeLockAspectTest | 6 | 0 | 0 | 0 |
| LocalLockManagerTest | 6 | 0 | 0 | 0 |
| WorkbenchApplicationHttpTest | 1 | 0 | 0 | 0 |
| WorkbenchIntegrationTest | 21 | 0 | 0 | 0 |
| WorkbenchKnowledgeServiceTest | 6 | 0 | 0 | 0 |
| CommerceControllerTest | 4 | 0 | 0 | 0 |
| CommercePersistenceTest | 14 | 0 | 0 | 0 |
| DemoBusinessTimeTest | 4 | 0 | 0 | 0 |
| 合计 | 62 | 0 | 0 | 0 |

统计来自本机 `backend/target/surefire-reports/TEST-*.xml`，完整日志为忽略目录中的 `backend/target/handoff-regression-tests.log`。持久化故障用例故意触发数据库约束异常，套件仍全部通过。测试不连接实际模型；真实 MySQL HTTP 和双端浏览器范围另见 [本轮验证](verification.md)。
