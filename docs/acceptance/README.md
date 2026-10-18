# C 成员验收入口

按以下顺序使用：

1. `node demo-data/scripts/validate.mjs` 检查数据。
2. `node docs/acceptance/scripts/preflight.mjs docs/acceptance/evidence/preflight-latest.json` 检查本机环境。只读访问知识总览与前端页面，不创建会话、不调用模型。
3. 由 A/D 加载完整数据并提供真实接口，由 B 提供工作台和顾客入口。使用 [测试用例](TEST-CASES.md) 执行业务验收。
4. 在 [验收记录](RESULTS-2026-10-18.md) 中逐项记录；修复后新建带日期的运行记录，保留原记录。
5. 问题按 [问题清单](ISSUES.md) 复验；完整验收通过后按 [录像脚本](../demo/RECORDING.md) 录制。

状态定义：

| 状态 | 含义 |
|---|---|
| PASS | 在实际运行系统上完成步骤，所有预期均满足，证据可回查 |
| FAIL | 在实际运行系统上执行后观察到与预期不符，附最小复现与证据 |
| BLOCKED | 前置环境/功能不具备，不能执行或完成；不能算作已通过 |
| NOT_RUN | 条件具备但尚未执行 |

源码检查只写入“静态发现”，不能据此把 AC 标成 PASS 或运行失败。数据校验通过只证明 fixture 自洽。

## 证据命名

每次验收创建独立目录 `evidence/YYYY-MM-DD-runNN/`。记录 Git commit、未提交变更范围、测试者、Asia/Shanghai 时间、LLM 模式与模型名、实际前后端地址、数据包版本/摘要、演示时间策略。

建议每条证据以 `AC-08-create-response.json` / `AC-08-ticket-detail.png` 等命名。保存会话 ID、消息请求 ID、实际工单号、工具执行记录与前后状态快照。只保留必要的演示数据；HTTP 证据剔除 Authorization 和配置密钥。

## 结果记录模板

| 项目 | 填写内容 |
|---|---|
| 用例 / 运行时间 / 测试者 | AC-xx / yyyy-MM-dd HH:mm:ss +08:00 / 姓名 |
| 版本 / 环境 | commit、脏工作区说明、地址、LLM 模式 |
| 数据前置 | 店铺、顾客、订单 ID、实际会话 ID |
| 实际操作 | 原始消息、请求去重键、人工操作、刷新/重启步骤 |
| 预期 / 实际 | 分别记录，不能复制预期作为实际 |
| 状态 | PASS / FAIL / BLOCKED / NOT_RUN |
| 证据 / 问题 | 相对路径、工单号、问题编号 |
| 复验 | 修复版本、复验时间、复验结果 |

本目录脚本退出码：`preflight` 为 0（前后端基线可访问）/2（未就绪）；异常脚本故障为非零。预检成功仍需执行全部业务用例。
