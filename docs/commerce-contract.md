# D 成员电商业务接口契约

基于需求 v0.2，2026-10-18。入口为 `/api/commerce`，全部使用 `POST` 和 JSON，后端端口默认 `10002`。新增业务 ID 均为字符串。成功沿用 `R`：`{"code":0,"message":"操作成功","data":{},"timestamp":...}`。

## 身份与调用边界

本模块是单商家本地 Demo 的业务后端。HTTP 接口接受显式范围，校验商家→店铺→顾客关系以及订单/工单归属。**范围校验不等于登录认证**：目前项目没有生产身份认证，这些直接接口供本地商家工作台和联调使用。

顾客聊天工具由 A 根据已校验的会话构造 `scope`，不能采用模型或浏览器自行声明的身份。A 的工具调用 Java 服务，不应把顾客消息直接转发给 `/tickets/create`。商家操作中的 merchantId、shopId、operatorId 也应在后续认证接入时由后端确定。

```json
{"merchantId":"demo-merchant","shopId":"demo-shop-tb","customerId":"demo-customer-tb-1"}
```

传入不存在或归属错误的范围统一返回 `NOT_FOUND`。指定外店/外顾客订单号的查单返回空匹配；物流和创建工单返回同样的 `NOT_FOUND`，不区分资源不存在与无权访问。

## 接口清单

| 路径 | 请求 | 返回 data |
|---|---|---|
| `/orders/query` | scope，选填 orderId、orderNumber、productClue | matchStatus、orders、message |
| `/logistics/query` | scope、orderId | available、unavailableReason、carrier、trackingNumber、status、nodes |
| `/tickets/create` | scope、sessionId、messageId、orderId、ticketType、reason、customerConfirmed | ticket、created、reuseReason、message |
| `/tickets/query` | scope，选填 ticketNumber、orderId、sessionId | 工单数组，含处理历史 |
| `/tickets/update` | merchantId、shopId、ticketId、expectedStatus、status、note、operatorId | 更新后的工单，含完整处理历史 |
| `/demo/identities` | 无请求体 | merchantId、merchantName、shops（每店包含 customers）、businessTime（mode、effectiveAt、timeZone） |
| `/demo/load` | 无请求体 | 加载结果；补齐缺失演示数据，保留现有订单及工单 |
| `/demo/restore` | `{"confirmed":true}` | commerce 演示业务恢复结果；清除演示工单 |

必填 ID 非空且不超过 64 字符，productClue 不超过 100 字符，reason/note 非空且不超过 1000 字符。枚举区分大小写。缺少确认字段按未确认处理。所有日期为当地 `Asia/Shanghai` 的 ISO 本地时间，如 `2026-10-10T09:00:00`；演示数据时间固定，不用于实时到货预测。

2026-10-20 新增的 `businessTime.effectiveAt` 带明确 UTC 偏移，如 `2026-10-18T12:00:00+08:00`。默认固定演示活动有效期时钟，双端据此提示；可通过 `DEMO_TIME_MODE` 和 `DEMO_REFERENCE_TIME` 调整。订单/物流日期不平移，工单操作时间仍为真实时间，配置不代表自动批准退货或退款。

## 订单与物流

```json
{
  "scope":{"merchantId":"demo-merchant","shopId":"demo-shop-tb","customerId":"demo-customer-tb-1"},
  "productClue":"杯"
}
```

多条件取交集。orderNumber 是准确单号，orderId 是内部字符串 ID；productClue 按商品名称及规格做不区分大小写的字面子串匹配，不把 `%`/`_` 当通配符。空条件列出该顾客所有订单。自然语言中的商品线索由 A 提取，例如“我的杯子到哪了”可转成“杯”。

返回 `matchStatus`：`NOT_FOUND`（0 笔）、`MATCHED`（1 笔）、`NEED_SELECTION`（多笔）。多笔时 orders 是待选列表，A/B 必须要求选择后再查物流或提交工单；A 把已选 orderId 保存进会话上下文。结果按下单时间倒序、ID 正序排列。

```json
{
  "matchStatus":"NEED_SELECTION",
  "orders":[
    {"orderId":"demo-order-tb-02","orderNumber":"TB202610110002","merchantId":"demo-merchant",
     "shopId":"demo-shop-tb","shopName":"安静生活淘宝店（演示）","platform":"TAOBAO","customerId":"demo-customer-tb-1",
     "items":[{"itemId":"demo-order-tb-02-item-1","productId":"demo-product-cup","productName":"保温杯","specification":"黑色 750ml","quantity":1,"unitPrice":69.00}],
     "totalQuantity":1,"actualPaid":69.00,"status":"PENDING_SHIPMENT","orderedAt":"2026-10-11T10:00:00"},
    {"orderId":"demo-order-tb-01","orderNumber":"TB202610100001","merchantId":"demo-merchant",
     "shopId":"demo-shop-tb","shopName":"安静生活淘宝店（演示）","platform":"TAOBAO","customerId":"demo-customer-tb-1",
     "items":[{"itemId":"demo-order-tb-01-item-1","productId":"demo-product-cup","productName":"保温杯","specification":"白色 500ml","quantity":1,"unitPrice":59.00}],
     "totalQuantity":1,"actualPaid":59.00,"status":"SHIPPED","orderedAt":"2026-10-10T09:00:00"}
  ],
  "message":"找到多笔订单，请顾客选择后再继续"
}
```

订单状态：`PENDING_SHIPMENT`、`SHIPPED`、`DELIVERED`、`CANCELLED`。物流以 orderId 查询，返回真实存储的节点，按 occurredAt 升序。同时间按 nodeId 排序。未发货/取消/缺物流记录时 `available=false`，carrier/trackingNumber/status 为 null、nodes 为空，并提供 unavailableReason。无 ETA 字段，不推测送达时间。

## 本地服务工单

```json
{
  "scope":{"merchantId":"demo-merchant","shopId":"demo-shop-dy","customerId":"demo-customer-dy-1"},
  "sessionId":"demo-session-dy-1",
  "messageId":"customer-message-001",
  "orderId":"demo-order-dy-01",
  "ticketType":"RETURN_REQUEST",
  "reason":"杯盖破损，申请退货",
  "customerConfirmed":true
}
```

类型：`RETURN_REQUEST`（退货诉求）、`EXCEPTION_COMPENSATION`（异常/补偿诉求）。成功才有真实的 ticketId 和 ticketNumber；首次创建状态 `PENDING_REVIEW`。确认表达应来自顾客已发送并持久化的消息，不能由模型推断成 true。独立商家接口只校验显式确认；聊天确认的真实性由 A 的 guard 校验。

`TicketCreated` 返回 `ticket`（完整工单）、`created`（是否本次新建）、`reuseReason`（新建为 null，重试为 `MESSAGE_RETRY`，复用未结单为 `OPEN_SAME_TYPE`）和准确提示文案。工单包含 ticketId/ticketNumber、merchantId/shopId/customerId/orderId/orderNumber/sessionId、ticketType、reason、status、createdAt、updatedAt、history。history 每项含 historyId/fromStatus/toStatus/note/operatorId/occurredAt。初始记录 fromStatus 为 null。

`platformAfterSaleStatus` 固定为 `NOT_SUBMITTED_TO_PLATFORM`。工单完成仅表示本系统处理记录完成。提示为“已记录诉求，待商家审核；本系统未执行平台退货或退款”。重试读取工单当前状态，不再重复声称“待审核”。

去重规则：

1. 同一 merchant/shop/customer/session/message 的请求保存提交回执，旧消息在工单终态后重试仍返回同一工单。改变 orderId/type/去首尾空格后的 reason 会返回 `IDEMPOTENCY_CONFLICT`，不能拿原消息编号提交新内容。
2. 同一顾客、订单、类型已有 `PENDING_REVIEW` 或 `PROCESSING` 工单时，保守合并到该工单，避免反复建单；新原因不会覆盖原原因。需要区别处理的补充诉求交商家判断。
3. 每个复用旧工单的新消息也保存回执。新会话中重复提出未结诉求后，按该会话查询可找到同一工单，ticket.sessionId 保留最初创建会话。
4. 同一类型工单终结后，新的消息可以新建；另一类型可以独立建单。

创建、回执和初始处理历史在同一事务中提交；失败全部回滚。以商家行的 `PESSIMISTIC_WRITE` 锁串行化创建、更新及恢复，在多实例间生效。提交回执使用 SHA-256（长度前缀编码）的主键，数据库主键防止重复回执。工单号有唯一约束。工单数量为 Demo 规模，商家级串行锁优先保证正确性。

查询过滤器取交集，空过滤器列出该顾客所有工单。工单号跨顾客、跨店时返回空数组。按 sessionId 可匹配初始会话及提交回执中的复用会话；订单确认上下文仍由 A 负责。

商家状态更新示例：

```json
{
  "merchantId":"demo-merchant","shopId":"demo-shop-dy","ticketId":"替换成返回的 ticketId",
  "expectedStatus":"PENDING_REVIEW","status":"PROCESSING",
  "note":"已联系顾客核实破损情况，等待商家审核处理方案","operatorId":"demo-operator"
}
```

允许 `PENDING_REVIEW → PROCESSING → COMPLETED`，以及 `PENDING_REVIEW → REJECTED`。禁止跳过处理中直接完成、处理中拒绝和终态重开。每次变更必须填写说明。expectedStatus 避免并发覆盖，旧状态请求返回 `STALE_STATUS`。新状态、时间和处理记录一起持久化。

## A 的 Java 接入

注入 `CommerceService`：`getOrder`、`getLogistics`、`getServiceTicket` 可直接调用。写入必须调用 `createAgentTicket`，默认缺少 `AgentTicketGuard` Bean 时抛 `AGENT_GUARD_UNAVAILABLE`，不会创建工单。

A 实现唯一的 `AgentTicketGuard` Bean，其 `lockAndVerify(CreateTicket)` 在 commerce 的事务内执行：

1. 使用数据库悲观写锁锁定 request.sessionId 对应会话，锁保持至外层事务完成；校验会话未关闭且接待模式是 AI。
2. 根据会话的真实 merchant/shop/customer 绑定比对 scope；校验 messageId 确属该会话、该顾客。
3. 核对持久化的已选订单、待提交类型/原因及顾客确认消息。接管失效的旧轮次也必须拒绝。
4. 人工接管、交回 AI、关闭会话竞争同一会话锁，不能只在事务外检查模式或相信模型传来的模式。

锁顺序为“会话 → 商家 → 工单/回执”，A 的其他写流程必须保持一致。需要同事务写执行记录时，A 的外层服务使用 `@Transactional` 调用 commerce 服务，记录实际返回编号；失败执行记录须在失败事务结束后用独立事务保存。不要在回滚事务中记录成功，不要持有数据库锁等待模型响应。回复发送前仍需 A 检查轮次及接待模式。

`createTicket` 与直接 HTTP 创建是商家本地联调能力，不承担 AI 会话授权，不能用来绕过上述校验。A 接入后，B 的顾客入口应使用 A 的会话接口。

## 演示数据与恢复

种子文件：`backend/src/main/resources/commerce/demo-data.json`，C 可按其中的结构维护。loader 校验主键、关系、金额、数量及节点字段后加载，操作有事务保护。没有启动时自动写库的行为；启动后调用 `/demo/load`。MySQL 表由现有 JPA `ddl-auto=update` 建立。

| 店铺 | 顾客 | 订单 |
|---|---|---|
| demo-shop-tb | demo-customer-tb-1（小林） | demo-order-tb-01～04；两笔保温杯需选择 |
| demo-shop-tb | demo-customer-tb-2（小周） | demo-order-tb-05～06；05 为物流异常 |
| demo-shop-dy | demo-customer-dy-1（小林） | demo-order-dy-01～03；01 可演示退货 |
| demo-shop-dy | demo-customer-dy-2（小陈） | demo-order-dy-04～06；04 有两项商品 |

商家固定 `demo-merchant`。两店有同名顾客和同样的 platformUserId（buyer-001/002），业务身份必须使用 customerId 并绑定 shopId。三种商品为保温杯、收纳盒、毛巾；productId 是 commerce 商品引用，不替换知识模块的数值 ID，A/C 需显式关联知识。

`load()` 补齐缺失记录，不覆盖现有演示工单/订单。`restore(true)` 清除该演示商家下标记 demoData 的提交回执、工单、物流和订单，按种子恢复基础身份/订单/物流；保留非演示数据。演示 ID 与非演示记录冲突，或非演示记录引用将删除的演示资源时拒绝并回滚。恢复不访问 chat、knowledge 或执行日志表。

**这只是 commerce 业务恢复，系统完整重置由 A 提供统一入口**。A 应先阻止该演示空间的新写入、等待在途操作结束，再在统一事务中清理自己的演示会话/上下文/执行记录并调用 `restore(true)`，完成后通知 B 刷新。首次同时加载缺失的同一商家时可能有一方唯一约束失败，原请求可重试，不会产生重复商家或订单。

## 错误契约

失败 HTTP 状态与 `R.code` 一致，`data` 为 `{"errorCode":"..."}`；没有伪造业务编号。

| HTTP | errorCode | 含义 |
|---|---|---|
| 400 | INVALID_ARGUMENT | 缺字段、长度错误、未知枚举、JSON 错误或未显式确认 |
| 400 | CONFIRMATION_REQUIRED | Java 业务调用缺少确认 |
| 404 | NOT_FOUND / DEMO_NOT_LOADED | 当前范围无资源 / 尚未加载演示数据 |
| 409 | IDEMPOTENCY_CONFLICT | 同一消息内容变更 |
| 409 | STALE_STATUS / INVALID_TRANSITION | 旧状态 / 不允许的状态迁移 |
| 409 | DEMO_DATA_CONFLICT | 恢复可能影响非演示数据 |
| 503 | PERSISTENCE_FAILED | 数据库/事务失败；使用原消息重试或查询确认 |
| 503 | AGENT_GUARD_UNAVAILABLE | A 尚未接入 AI 写入校验 |

Java 服务抛 `CommerceException`，可读取 errorCode；Bean Validation 错误为 ConstraintViolationException。数据库提交失败会抛异常，不返回成功对象。若响应在提交后丢失，用相同 messageId 重试恢复已提交结果。
