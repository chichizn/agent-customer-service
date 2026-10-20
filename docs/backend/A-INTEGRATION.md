# A 成员后端集成交付与联调说明

更新：2026-10-20。基于 `a2f67e0` 的 B 工作台、C 集成知识和 D commerce 服务，补充 A 的会话、业务编排及人工协作链路。最新验证范围见[验收记录](../acceptance/RESULTS-2026-10-20.md)。

## 交付范围

| 模块 | 本次实现 |
|---|---|
| 会话 | 固定绑定演示商家、店铺和顾客；恢复最近 active 会话或显式新建；列表、快照、消息和上下文持久化 |
| 查询 | 调用 D 的实际服务查询本人订单、澄清多笔订单、查询物流和跨会话工单；身份从后端会话读取 |
| 售后 | 补齐订单、诉求与原因，顾客明确确认后，经 `SessionTicketGuard` 调用 `createAgentTicket`；返回真实本地工单号 |
| 幂等 | 同一会话/角色/`clientMessageId` 唯一；改内容返回 409；未结同类工单复用 D 的防重机制 |
| 人工协作 | WAITING_HUMAN、HUMAN、AI；交接摘要保留顾客原文，接管等待人工会话时保留原摘要；接管后阻止迟到回复和写入，交回后等下一条顾客消息 |
| 知识 | 构建时打包 C 的 10 条集成知识；导入、ACTIVE/有效期过滤、商家及店铺范围检索、快照引用 |
| 执行记录 | 记录实际知识/订单/物流/工单查询、建单、商家状态更新和接管；业务回滚后尝试另存失败记录 |
| 重置 | 经明确确认统一恢复演示 commerce、知识和会话；保留非演示数据；旧会话及处理中的请求失效 |

当前采用有限业务状态编排，LLM 提供意图提示；工具参数中的身份、写入确认和最终业务结果由服务端决定。业务回复直接组织实际工具结果和检索原文，未实现开放式多工具自主规划，也没有让模型自行批准退款或赔偿。

run11增加 `MerchantTicketService`，`/api/commerce/tickets/update` 通过该服务将commerce状态、历史与会话执行记录放入同一 `REQUIRES_NEW` 事务；失败再开事务记录FAILED。先锁merchant再锁session，与消息/reset锁顺序一致；工单绑定会话且merchant/shop/customer匹配才写执行记录。完整存储不可用时FAILED也可能无法落库。新增回归及真实MySQL触发器故障验证覆盖成功记录失败导致整体回滚、恢复重试和伪造店铺不污染记录；后端全量83项通过，另有17组可靠性HTTP检查，见[证据与限制](../acceptance/evidence/2026-10-20-run11/verification.md)。

原聊天入口仍保留。工作台使用新增快照和持久上下文；旧聊天入口的 `context/get`、`context/update` 也会读写会话上下文，便于历史前端继续使用。原消息发送和删除接口拒绝绕过工作台处理业务会话。

## 接口契约

均为 POST，沿用 `{ code: 0, msg, data }` 成功外层；前端也兼容历史 `code: 200`。时间字符串使用 ISO 8601，工作台新增时间带 `+08:00`。ID 为字符串，金额为元。

| 路径 | 请求 | data / 行为 |
|---|---|---|
| `/api/chat/session/list` | `{ "workbench": true }` | 工作台会话数组；不带标记沿用旧聊天列表 |
| `/api/chat/session/open` | `{ "shopId", "customerId", "forceNew": false }` | 快照；false 恢复最近会话，true 显式新建 |
| `/api/chat/session/snapshot` | `{ "sessionId" }` | 消息、订单、物流、工单、引用、执行记录及待确认状态 |
| `/api/chat/message/send` | `{ "sessionId", "clientMessageId", "content" }` | 持久化顾客消息并返回处理后的快照 |
| `/api/chat/message/human/send` | 同上 | 仅 HUMAN 可发送人工消息 |
| `/api/chat/session/reception/update` | `{ "sessionId", "receptionMode": "HUMAN" }` | HUMAN 接管或 AI 交回；WAITING_HUMAN 由服务端触发 |
| `/api/demo/reset` | `{ "confirmed": true }` | 统一重置；未确认返回 400 |

`sessionId`、`clientMessageId` 最长 64 字符且必填，消息内容最长 4000 字符，诉求原因最长 1000 字；原因超长会要求重新补充。会话处理中的并发普通发送返回 409，完成后可使用原编号重试取得同一结果；原编号不能更改内容。失败确认只能在原业务上下文重试，诉求变更后必须重新确认，旧编号返回 409。服务端处理租约为 60 秒，轮次版本阻止过期结果回写。

顾客选择订单时，发送返回的订单 ID 或订单号。确认按钮发送“确认提交”；也接受“确认提交退货诉求。”等明确表达，路由与写入 guard 共用同一确认校验。取消、改查物流、改问规则或人工切换会撤销旧待确认诉求。

400 表示参数/业务确认不符，404 表示当前范围内不存在，409 表示冲突或仍在处理，503 表示业务持久化失败。失败响应不会返回虚假的建单成功；前端保留原编号重试，并可刷新执行记录核对。

## 启动与最小联调

1. 按根 README 配置 JDK 17、MySQL 与本地 `application-local.yml`，启动后端。保留已有配置，密钥只放本地。
2. 首次调用 `POST /api/commerce/demo/load`，请求 `{}`。该操作只补缺；工作台首次打开会导入 C 知识。日常统一重置使用工作台的重置确认按钮。
3. 前端设置 `VITE_CUSTOMER_SERVICE_MODE=api`，重启 Vite。工作台和顾客入口沿用 B 文档的 Hash 路由；默认仍为 mock，api 请求失败直接报错。
4. 完整退货演示需开启且能访问实际模型。模型关闭或请求失败时，查询和人工功能仍可用，写诉求转人工，不伪装为模型正常运行。

可在 PowerShell 使用以下请求验证真实后端。它会在当前演示数据库中建立会话；确认两步会创建本地工单。

```powershell
$base = 'http://localhost:10002'
function Invoke-DemoPost($path, $body) {
  $json = $body | ConvertTo-Json -Depth 10
  Invoke-RestMethod -Method Post -Uri ($base + $path) -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($json))
}
Invoke-DemoPost '/api/commerce/demo/load' @{}
$opened = Invoke-DemoPost '/api/chat/session/open' @{
  shopId='demo-shop-dy'; customerId='demo-customer-dy-1'; forceNew=$false
}
$session = $opened.data.session.sessionId
Invoke-DemoPost '/api/chat/message/send' @{
  sessionId=$session; clientMessageId=[guid]::NewGuid().ToString()
  content='想退货 DY202610100001，原因是杯盖漏水'
}
# 核对返回的订单、原因与 awaitingConfirmation 后再发送确认。
Invoke-DemoPost '/api/chat/message/send' @{
  sessionId=$session; clientMessageId=[guid]::NewGuid().ToString(); content='确认提交'
}
```

人工接管不等待模型请求完成。数据库锁统一按商家、会话的顺序获取；模型调用放在事务外。建单、工单提交记录、成功执行记录、回复和确认状态在同一完成事务提交；其中任何一步失败都会回滚，失败记录使用新的事务写入。

LLM 的连接超时为 3 秒、读取超时为 8 秒；这不是对 DNS 或整个请求的硬性总时长保证。模型失败时快照/回复明确显示规则降级，写入诉求等待人工。真实网络超时和坏响应仍需环境验收。

## 演示业务时间

`DemoBusinessTime` 默认固定在 `2026-10-18T12:00:00+08:00`，由 `DEMO_TIME_MODE=fixed|system` 和 `DEMO_REFERENCE_TIME` 配置。身份接口新增 `businessTime: { mode, effectiveAt, timeZone }`，工作台及顾客端显示其含义。仅演示知识有效期使用演示时钟，非演示知识始终按真实时间；消息、工单、轮次租约和请求超时保持真实时间。种子订单/物流不平移，当前不自动判定退货资格或批准售后。

`cs_scoped_knowledge` 新增 `kind`、`starts_at`、`ends_at`，开发环境由既有 JPA `ddl-auto=update` 补列。打开/恢复会话时对旧演示知识补齐元数据，保留已有文案、状态及范围；无需 reset。活动两端均包含，缺少完整日期或已经过期的活动不参与检索和快照引用。实际 MySQL 升级及活动过滤证据见 [run06](../acceptance/evidence/2026-10-20-run06/verification.md)。

## 验证记录与剩余工作

在本机 JDK 17、Maven 3.9.9、Node 24.19.0、pnpm 10.10.0 下执行：

```powershell
cd backend
mvn -o -B test
# 另一个终端，仓库根目录：
cd frontend
pnpm test:workbench
pnpm build
```

- 后端当前全量：62 项通过，包括交接摘要保留、直接接管撤销授权和下一轮更新，以及时钟、活动有效期、旧知识升级、锁、事务、commerce、guard 测试；LLM 使用替身或关闭。最新执行证据见 [run07 验证](../acceptance/evidence/2026-10-20-run07/verification.md)。
- 独立 MySQL 8.0.42 验收库完成34项只读 commerce HTTP 检查及6组工作台 HTTP 冒烟；当前源码重启后复跑通过，5个原会话完整快照重启前后一致。范围和限制见[run04/run05记录](../acceptance/RESULTS-2026-10-20.md)；不会据此声称完整 AC 已通过。
- A 测试涵盖身份隔离、多单澄清、未确认零写入、持久恢复、重复消息/工单、商家更新后追问、知识范围、HTTP 契约/参数错误、人工幂等、处理中接管、重置阻止迟到写入、降级和取消/切换诉求。
- 故障测试在实际插入工单和提交记录后，由数据库约束使执行记录写入失败，验证整轮业务回滚、无成功回复、保留失败记录、同一消息编号重试恢复。日志中的约束错误为该测试预期结果。
- 前端：14 项工作台/commerce 适配测试、Vue 类型检查与 Vite 生产构建通过（run06）；run07浏览器实测双端时间提示、人工消息同步、接管/交回与刷新恢复。顾客刷新后需明确点击“恢复最近会话”；有限冒烟不代表完整业务AC。

这些结果包括自动化模块验证及有限范围的真实 MySQL HTTP 验证；仍未执行实际模型、浏览器双端和重启后的完整 AC-01～AC-22。C 的原验收记录保留其当时状态，不因本次代码补齐而改成全通过。

后续在真实环境跑三个故事和 22 项 AC，核对双端同步、含工单与浏览器的完整恢复、实际模型故障及并发；TB3 无订单顾客、固定演示活动时钟及有效期过滤已加入。当前知识保留 C 原文及日期，不自动判断退货资格或批准补偿。演示单商家身份固定，生产认证、坐席权限和真实平台操作仍在本版范围外。
