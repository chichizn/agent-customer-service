# C 成员验收入口

最新状态见 [2026-10-19 验收记录](RESULTS-2026-10-19.md)；保留 [10月18日记录](RESULTS-2026-10-18.md)。

## 执行顺序

1. 同步 main，记录 commit 与本地改动。以 `demo-data/integration.v1.json` 对应 D 种子，不混用 C 旧草案或 B mock 身份。
2. 根目录运行离线检查，无需第三方依赖。
3. 启动后端，由操作者加载 D 数据后运行预检和 commerce 只读检查。前端 API 模式需 A 会话实现；前端模拟可单独预览。
4. 按 [TEST-CASES.md](TEST-CASES.md) 执行22条业务验收，保存真实会话、消息、工单、执行记录及前后状态。
5. 新建带日期结果，按 [ISSUES.md](ISSUES.md) 复验；三个故事连续可操作后按 [录像脚本](../demo/RECORDING.md) 录制。

```powershell
node demo-data/scripts/check-integration.mjs docs/acceptance/evidence/integration-latest.json
node --test docs/acceptance/scripts/integration.test.mjs
node docs/acceptance/scripts/preflight.mjs docs/acceptance/evidence/preflight-latest.json
node docs/acceptance/scripts/commerce-readonly.mjs docs/acceptance/evidence/commerce-latest.json
```

接口脚本只访问本机10002的4类 POST 查询：身份、订单、物流、工单；检查四位顾客归属、订单/物流内容、多单/单号匹配、外店/外顾客/不存在订单不可见。不加载/重置、不创建工单、不调用模型。工单为空时只能证明查询可用，不能计为建单或工单隔离已验收。显式 scope 也不能代替 A 会话身份校验。

| 检查 | 退出码 | 通过含义 |
|---|---|---|
| check-integration | 0 / 1 | 离线数据与 C 场景/知识一致 / 有错 |
| integration.test | 0 / 非0 | 检查器自身回归通过 / 失败，使用替身 |
| preflight | 0 / 2 | 前后端基线可访问 / 未就绪，响应详情见报告 |
| commerce-readonly | 0 / 1 / 2 | 查询响应符合种子 / 响应不符 / 不可达或超时 |

旧数据/加载器回归用 `node demo-data/scripts/verify-package.mjs`；不传输出路径可避免覆盖历史证据。

## 业务结果定义

| 状态 | 含义 |
|---|---|
| PASS | 在实际系统完成步骤，全部预期满足，证据可回查 |
| FAIL | 执行后观察到与预期不符，有最小复现 |
| BLOCKED | 前置环境/功能不足，无法执行或完成 |
| NOT_RUN | 条件具备但尚未执行 |

源码发现、模块单测、B mock、离线校验分别记录，不能据此将完整 AC 标成 PASS。

## 证据与记录模板

每轮创建 `evidence/YYYY-MM-DD-runNN/`，记录 commit、本地改动、测试者、Asia/Shanghai时间、LLM模式/模型名、地址、种子摘要、日期策略。链接对应证据，不覆盖历史。

| 字段 | 必填 |
|---|---|
| 用例 / 时间 / 测试者 | AC-xx、时间与人员 |
| 版本 / 环境 | commit、本地变更、地址、LLM模式 |
| 数据前置 | 店铺/顾客/订单、真实会话 ID |
| 实际操作 | 消息、去重键、人工操作、刷新/重启 |
| 预期 / 实际 | 分开记录，不能复制预期充当结果 |
| 状态 / 证据 / 问题 | 实际状态、路径、真实编号、问题号 |
| 复验 | 修复版本、时间及结果 |

HTTP 证据不保存 Authorization、密码或模型密钥。执行记录不能用模型推理文字代替。
