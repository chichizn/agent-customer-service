# 演示交换格式与 A/D 接入说明

版本 `1.0.0`；这是 C 编制的数据格式草案，**不是已实现的 API 契约**。A/D 的真实字段模板到位后，以映射方式调整数据；本次不修改后端和公共配置。

## 通用约定

| 字段 | 约定 |
|---|---|
| `id` | 稳定字符串；`demo-` 前缀；重复导入按此键更新/复用，不能每次产生新业务对象 |
| `demoNamespace` | 固定 `customer-service-demo-v1`，标记受统一重置管理的演示空间 |
| `merchantId` / `shopId` / `customerId` | 外键必须真实存在且归属一致；服务端从已验证会话取得工具的实际身份 |
| `platform` | `TAOBAO` / `DOUYIN`；展示为淘宝/抖店；如最终枚举不同，由 A/D 显式映射 |
| 金额 | `*Cents` 为整数分，币种 CNY；当前 Product DTO 的 `price` 为元，仅基线加载器除以 100 |
| 日期 | ISO 8601，明确 `+08:00`；后端 LocalDateTime 按 Asia/Shanghai 解析后存储，不保留偏移字符串 |

`demoNamespace` 及 ID 前缀不能成为任意客户端可修改的“授权”。演示加载/重置入口的访问控制由 A 实现；不能根据用户输入的 merchantId 清库。

## 对象字段

| 数组 | 主要字段 | 加载责任与现状 |
|---|---|---|
| `merchants` | id, name | A/D 演示基础数据；待最终契约 |
| `shops` | merchantId, platform, name | D 基础数据，A 用于会话身份校验 |
| `customers` | merchantId, shopId, platformUserId, displayName | A/D 映射；唯一身份组合至少包括店铺 |
| `products` | productCode, productName, listPriceCents, description, specifications, features | D 保持字符串 fixture ID 到现有数字商品主键的映射；不可强占原项目主键 |
| `orders` | orderNumber, 身份归属, items, discountCents, paidAmountCents, currency, status, orderedAt | D 新增订单业务；单价×数量之和减优惠等于实付 |
| `orders.items` | productId, productName, quantity, unitPriceCents | 商品引用是 fixture ID，D 使用映射表替换；历史单价不受当前活动修改 |
| `orders.platformAfterSaleStatus` | 初始 NONE | 仅演示平台状态；本地建工单不能改成平台退款成功 |
| `logistics` | orderId, 身份归属, carrier, trackingNumber, status, estimatedDeliveryAt, nodes | D；无物流的订单没有此行，不能生成运单 |
| `logistics.nodes` | occurredAt, description | 时间升序；`estimatedDeliveryAt:null` 表示未知，不承诺 ETA |
| `knowledge` | merchantId, scope, shopId, kind, topic, title, content, source, productIds, policy, status | A/D 映射到现有知识及店铺规则；SHOP 只能当前店使用，MERCHANT 为明确共用 |
| `sessions/messages/serviceTickets/executionRecords` | 初始空数组 | A/D 运行时创建，不预填“工单成功”等结果 |

商品是商家共用目录；每家店的订单引用相同商品 ID，体现同名商品不能替代店铺归属。`knowledge.policy` 是便于校验的结构化补充，正文 `content` 同时保留可引用的完整规则。kind 为 PRODUCT / FAQ / SHOP_RULE / ACTIVITY，不强制成为最终数据库枚举。

订单状态与需求文档一致；物流状态采用 IN_TRANSIT / DELIVERED / DELAYED，是 fixture 约定，需要 D 映射。`scenario` 只用于数据说明，不能作为 Agent 硬编码路由条件。

## 时间与重复恢复

1. 取实际演示时刻 T，计算 `T - meta.referenceTime`。把下单时间、所有物流节点、活动开始及结束时间统一平移相同毫秒数。或者由 A 提供固定 referenceTime 的演示时钟，并在 UI 标明。二者选一，记录到验收环境。
2. 同一次加载中，数据库和应用必须使用同一演示基准。金额、相对时间、ID、店铺规则不随重置改变。
3. 同一 demoNamespace 重置由 A 统一协调：处理中的 AI 先失效，A 清理该空间会话/消息/上下文/执行记录，D 清理该空间工单并恢复基础数据。按实际外键顺序处理，不能跨模块随意删表。
4. 重复导入或重置后仍是 12 个订单、8 份物流、10 条演示知识；原项目知识等非演示数据保留。C 需先记录非演示数据的 ID 与内容快照，再比较。

## 最终联调前待 A/D 补齐的契约

- 完整数据加载和统一重置入口、返回统计、失败处理、日期平移方式、影响范围。
- 顾客身份列表、会话恢复/新建、增量消息轮询、接待模式与版本字段。
- 订单/物流/工单查询及写入 DTO、错误格式、去重键与订单确认/提交确认方式。
- 工具超时、处理中接管的取消/版本校验、失败注入方式、持久执行记录读取入口。

C 不预先编造这些 URL；联调时在验收记录中填写实际值，再执行 AC 用例。
