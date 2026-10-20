# 隔离 MySQL 可靠性验收

`AcceptanceRuntime.java` 和 `reliability-smoke.mjs` 是本机验收工具，不进入后端发行包。真实模型业务证据另见 run10、run11，不能将本工具的 fixture 输出称为供应商模型验收。

## 固定环境

- API：`127.0.0.1:10003`；模型 stub 与控制接口：`127.0.0.1:10004`。
- MySQL：`127.0.0.1:13307/agent_customer_service_acceptance_20261019`，本机隔离实例，root 空密码。每次控制端数据库连接检查实际端口和 schema；不会连接日常3306。
- 运行前独立创建上述实例和 schema；Java17+、Maven、Node20.19+，从仓库根目录操作。
- 控制接口无认证，只监听回环地址；不部署到公网。不要把本工具接到业务数据库。

## 启动和执行（PowerShell）

```powershell
mvn -f backend/pom.xml -q compile dependency:build-classpath "-Dmdep.outputFile=target/acceptance-dependencies.txt"
if ($LASTEXITCODE -ne 0) { throw 'Backend compilation failed' }
$acceptanceDependencies = (Get-Content backend/target/acceptance-dependencies.txt -Raw).Trim()
$acceptanceClasspath = (Resolve-Path backend/target/classes).Path + ';' + $acceptanceDependencies
java -Dfile.encoding=UTF-8 -cp $acceptanceClasspath docs/acceptance/scripts/AcceptanceRuntime.java --isolated-acceptance
```

看到 `ACCEPTANCE_RUNTIME_READY` 后，在仓库根目录另开终端：

```powershell
node docs/acceptance/scripts/reliability-smoke.mjs docs/acceptance/evidence/reliability-latest.json --allow-reset
```

脚本先核对目标、导出隔离库表的 JSON 快照，再创建非演示哨兵并重置演示数据。快照位于被忽略的 `backend/target/acceptance-backup-*`，供核查原始字段，**不是自动 SQL 还原工具**。报告保留请求键、错误、工单、会话和数量对照；无论成功与否均应检查报告的 checks，不能只看进程退出。

## 注入点及证据边界

| 模式 | 实现与限制 |
|---|---|
| valid | 本地 stub 返回合法 GENERAL_QUERY，再执行真实业务服务/MySQL |
| disabled | LlmService 验收包装器直接返回不可用，不发模型请求；不是切换真实服务 `llm.enabled=false` 的启动验证 |
| 500 / timeout / bad-json / unknown-intent | loopback 网关分别返回500、1800ms延迟、坏分析JSON、未知意图；后端连接500ms、读取1000ms |
| hold-model | 模型响应前暂停，方便复现处理中接管和响应丢失 |
| hold-before-finish | 模型返回后、业务 finish 事务前暂停，是写入前取消边界；不是已经持有商家锁后的暂停 |
| query fault | 代理 getLogistics 抛查询异常 |
| create / update fault | 仅对指定 session/action 的 SUCCESS 执行记录 INSERT 触发 MySQL45000，检查业务和历史一起回滚、另存FAILED、恢复后重试 |

`/control` 设置模式，`/release` 释放暂停，`/fault` 设置或清理故障，`/state` 查询实际目标和计数。暂停15秒自动到期。脚本 finally 会释放暂停并清理 trigger；手动中断时先执行：

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:10004/release -ContentType application/json -Body '{}'
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:10004/fault -ContentType application/json -Body '{"fault":"clear"}'
```

再关闭验收 Java 进程。完整数据库不可用时，FAILED 本身也可能无法落库，不能以本测试声称任何断库情况下都有失败记录。

## 真实模型开关关闭启动

使用上方生成的 `$acceptanceClasspath`，另行启动：

```powershell
java -Dfile.encoding=UTF-8 -cp $acceptanceClasspath docs/acceptance/scripts/AcceptanceDisabledRuntime.java --isolated-acceptance
```

该启动器仅监听127.0.0.1:10005，检查同一13307隔离库；直接使用原始 `LlmService`，显式传入 `llm.enabled=false`，启动后校验实际配置与Bean类型。使用非真实测试key与本机不可用模型地址，不读取 `application-local.yml`。看到 `DISABLED_ACCEPTANCE_READY` 后，可在10005用真实会话接口依次验证明确物流查询、写诉求转人工、人工回复、快照历史及工单查询；本轮实际请求/响应在 [disabled-config-live.json](../evidence/2026-10-20-run11/disabled-config-live.json)。完成后关闭该Java进程。
