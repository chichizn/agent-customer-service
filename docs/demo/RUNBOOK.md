# 演示运行与操作手册

更新：2026-10-19，main `99b5a48`。[最新结果](../acceptance/RESULTS-2026-10-19.md) 记录实际执行情况。当前 B 前端模拟可独立运行，D 已有 commerce 接口；A 会话、Agent、人工协作和统一重置尚未接入，完整故事仍待联调。

## 1. 选择运行方式

| 方式 | 数据和能力 | 可展示内容 |
|---|---|---|
| B 前端 mock | 浏览器 localStorage、前端规则回复，不调用 Java/MySQL/LLM | 页面、模拟多单、模拟工单、人工切换、同源双标签同步 |
| D commerce 检查 | Java/MySQL 订单、物流、工单接口 | 真实模块查询；不能代表聊天已调用工具 |
| 完整 api 模式 | B 页面 + A 会话/Agent + D commerce + 实际模型 | 下方三个业务故事；当前 A 接口缺失，尚不能完成 |

mock 的姓名/ID/商品另有一套，以 [B 说明](../frontend/B-WORKBENCH.md) 为准，不能在 mock 中输入下方 D 单号并声称查到数据库。

## 2. 启动与入口

需要 Node20.19+、项目指定 pnpm10.10.0。另开 PowerShell，在仓库根目录：

```powershell
Set-Location frontend
pnpm install --frozen-lockfile
$env:VITE_PORT = '20002'
$env:VITE_ACCESS_MODE = 'frontend'
$env:VITE_VERSION = '0.1.0-demo'
$env:VITE_CUSTOMER_SERVICE_MODE = 'mock'
pnpm exec vite --host 127.0.0.1 --port 20002 --strictPort
```

当前终端环境变量不改公共配置。Hash 路由必须保留 `#`：

- 工作台：`http://localhost:20002/#/customer-workbench-preview`
- 顾客：`http://localhost:20002/#/customer-simulator`

mock 可按 B 说明选择淘宝林小满、抖店周雨晴，展示页面内的实际候选，不套用 D 人物。两标签保持同一浏览器和源；模拟状态不跨设备同步。

本机曾因 pnpm11 的依赖构建审批规则导致安装退出1，已用已安装依赖的直接 Node 入口完成测试与打包。团队复现先使用仓库指定版本；不要把安装器退出失败写成业务失败。初次类型检查若缺自动导入声明，可先运行 Vite 生成，再检查；完整构建仍必须包含类型检查。

后端需 JDK17+、Maven3.8+、MySQL8，Redis 按项目配置。现会话 Java/Maven 不在 PATH，未启动后端。按 A 的实际环境配置启动，避免覆盖已有本地文件：

```powershell
if (!(Test-Path backend/src/main/resources/application-local.yml)) {
  Copy-Item backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
}
Set-Location backend
mvn spring-boot:run
```

启动前填写本地数据库/模型配置；不要把密钥写入验收文件或录像。D 数据不是自动启动加载，由操作者在正确数据库按 [commerce 契约](../commerce-contract.md) 调用 `POST /api/commerce/demo/load`。它补缺、不清除已有工单，不是完整重置。

A 接口就绪后把 Vite 的 `VITE_CUSTOMER_SERVICE_MODE` 改为 `api` 并重启。当前 api 失败会显示错误，不会回退模拟。根目录运行：

```powershell
node demo-data/scripts/check-integration.mjs
node docs/acceptance/scripts/preflight.mjs docs/acceptance/evidence/preflight-latest.json
node docs/acceptance/scripts/commerce-readonly.mjs docs/acceptance/evidence/commerce-latest.json
```

## 3. 完整演示前置

- A/D/B api 链路可运行，实际模型经过问答验证；关闭/失败时有明确降级。C 已按 [22条用例](../acceptance/TEST-CASES.md) 实测。
- 商家 `demo-merchant`。D 基线2店/4顾客/3种订单商品/12单/7物流；10条 C 范围知识另由 A 加载并给出映射，不能只数 JSON 行数。
- 统一重置由 A 协调，D restore 仅处理 commerce。确认影响范围再操作；另一标签旧任务失效。两次复验非演示数据保留。
- 日期为固定2026年种子，以2026-10-18 12:00+08为参考；当前2026-10-19退货单仍在窗口。后续录制须统一平移或固定演示时钟，并标明策略。
- 1440px桌面主验收，同时检查1280px。只展示真实查询/写入及实际编号，失败保留失败。

## 4. 真实数据速查

| 故事 | 身份 | 订单 |
|---|---|---|
| 多单物流 | 淘宝小林，demo-shop-tb / demo-customer-tb-1 | TB202610100001（demo-order-tb-01，运输中）与 TB202610110002（tb-02，750ml未发货） |
| 退货与进度 | 抖店小林，demo-shop-dy / demo-customer-dy-1 | DY202610100001（demo-order-dy-01，500ml杯，10-13 14:00签收） |
| 延迟转人工 | 淘宝小周，demo-shop-tb / demo-customer-tb-2 | TB202610120005（demo-order-tb-05，杯子延迟，10-14 16:00异常） |

完整单号、金额和状态见 [映射表](../../demo-data/FIELD-MAPPING.md)。抖店 dy-06 是运输中的毛巾，不能用作延迟故事；现种子没有无订单顾客。

## 5. 故事一：淘宝多单物流（约1分钟）

| 操作 / 台词 | 核对 |
|---|---|
| 选择淘宝小林恢复/新建会话，“我的杯子到哪了？” | 工作台出现对应会话，列出本人两笔杯子订单 |
| 选择 TB202610100001 | 不能自动选另一单，选单持久化 |
| “那这个订单现在到哪里了？” | 演示快递 / DEMO-TB-0001 / 2026-10-12 08:30杭州转运，来自实际工具 |
| “保证明天收到吗？” | 未提供ETA，明确未知，不承诺 |

完整模式讲解：“咨询由顾客模拟入口产生；订单和物流来自本地数据库的预置数据。”

## 6. 故事二：抖店退货与进度（约1分40秒）

| 操作 / 台词 | 核对 |
|---|---|
| 切到抖店小林，“订单 DY202610100001 的杯子漏水，想退货。” | 订单属于DY1，引用抖店演示规则 |
| 补充“杯盖拧紧仍漏水”，先不确认 | 还没有新工单 |
| “确认提交退货诉求。” | A 经 guard 调 D 创建 RETURN_REQUEST，真实编号记为TICKET-A；PENDING_REVIEW |
| “刚才的申请怎么样了？” | 仍为TICKET-A，状态真实 |
| 商家改 PROCESSING，备注“已联系顾客确认漏水情况” | expectedStatus为实际旧状态，历史与执行记录可查 |
| 顾客再次追问 | 同编号和最新说明；平台状态 NOT_SUBMITTED_TO_PLATFORM |

讲解：“已记录本地诉求，待商家处理；不是平台已退款。”保留会话供刷新/重启验证，不写死编号。缺少 A guard 时不能改用商家直建接口冒充聊天建单成功。

## 7. 故事三：延迟与规则外赔偿转人工（约1分20秒）

使用淘宝小周的新会话，避免退货上下文残留：

| 操作 / 台词 | 核对 |
|---|---|
| “订单 TB202610120005 的杯子物流一直没动，必须额外赔我500元，找人工。” | 查询实际延迟节点，WAITING_HUMAN，摘要完整，不承诺赔偿 |
| 商家接管，顾客连续问“有人吗”“现在处理了吗” | HUMAN 中AI不抢答、不继续建工单 |
| 商家回复“已收到，我来核实物流，赔偿需审核。” | 人工消息持久化并自动同步 |
| 显式交回AI，先观察，再发“这单物流有更新吗？” | 不重放旧消息，新消息触发，保留TB05上下文 |

讲解：“接待状态由后端保存，接管后停止AI回复和写入。”此说法仅用于实际后端验证后的完整演示。

## 8. 故障与恢复

| 情况 | 处理 |
|---|---|
| A 接口未接入 | 说明完整模式阻塞；可展示明确标为前端模拟的页面 |
| 模型失败 | 保留降级/失败标记，只展示已验证流程 |
| 提交后超时 | 先查实际工单/执行记录，使用原去重键重试 |
| 资料串会话 | 记录会话和请求，停止该段并交A/B复验 |
| 数据不符 | 核对模式、身份和种子，不临时编造记录 |
| 需要重置 | 只用A统一入口，查看范围并确认；不直接清空表 |
| 备用录像 | 只有已录制并回看验收的文件才能作为备用；当前尚无完整业务录像 |

结束时按 AC-22 连续重置复验，保留非演示资料快照。
