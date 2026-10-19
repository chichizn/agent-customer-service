# run03 后端自动化测试证据

日期：2026-10-19，Asia/Shanghai。基线：main `c2fac80` 加本轮 TB3 种子、测试和文档改动。

执行目录：`backend/`。命令：`mvn -o -B test`。JDK：17.0.12；Maven：3.9.9。

最终复跑于 23:18:01 +08:00 完成，退出码0，`BUILD SUCCESS`，耗时34.584秒。以下计数核对自本次 `target/surefire-reports/TEST-*.xml`，未复制报告中的环境变量、系统属性或本地凭据。

| 测试类 | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| WorkbenchApplicationHttpTest | 1 | 0 | 0 | 0 |
| WorkbenchIntegrationTest | 18 | 0 | 0 | 0 |
| CommerceControllerTest | 4 | 0 | 0 | 0 |
| CommercePersistenceTest | 14 | 0 | 0 | 0 |
| 合计 | 37 | 0 | 0 | 0 |

HTTP 测试以完整 Spring Boot 应用和随机端口验证身份列表、TB3 空订单会话、发送消息和会话列表的实际路由与 JSON 响应。使用独立 H2 内存库、关闭 LLM、排除 Redisson 自动配置并注入测试替身，不连接真实 MySQL/Redis。

日志中的数据库约束异常来自故障注入测试的预期回滚路径；所有测试最终通过。37项通过只代表自动化模块/接口验证，不代表真实 MySQL、实际模型、浏览器双端或 AC-01～AC-22 已验收。
