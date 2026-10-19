# C 与当前集成数据的映射

2026-10-19，基于 main `99b5a48`。已实现 commerce 契约见 [commerce-contract.md](../docs/commerce-contract.md)，B 对 A 的待实现约定见 [B-WORKBENCH.md](../docs/frontend/B-WORKBENCH.md)。

## 数据来源与字段

| 项目 | 当前 D 种子/接口 | C 历史草案 | 当前处理 |
|---|---|---|---|
| 商家 | demo-merchant | demo-merchant-01 | 使用 D 身份列表 |
| 店铺 | demo-shop-tb / demo-shop-dy | demo-shop-taobao / demo-shop-douyin | 新知识显式映射；旧包不直接加载 |
| 顾客 | demo-customer-tb-1/2、dy-1/2 | tb-01/02/03、dy-01/02/03 | 按真实身份读取，不能仅按姓名替换 |
| 商品 | cup、box、towel | cup、box、cloth、bag | 清洁布/帆布袋不等于毛巾，不作自动映射 |
| 杯子规格 | 白色500ml、黑色750ml | 原知识只写500ml | 新知识列出两规格；先选订单/规格再回答 |
| 订单 | demo-order-tb-01；TB202610100001 等 | demo-order-tb-001；DEMO-TB-20261018-001 等 | 使用下表，单号与 ID 分开 |
| 金额 | actualPaid、unitPrice：元，取消订单可为0 | paidAmountCents、unitPriceCents：整数分 | 映射时除以100 |
| 时间 | orderedAt、occurredAt 为无偏移 LocalDateTime | 明确 +08:00 | 统一解释 Asia/Shanghai；HTTP可用T或空格分隔 |
| 物流 | orders[].logistics，7条；status为中文描述 | 独立数组8条及英文状态 | 依照 available/原始节点，不编造运单或 ETA |
| 平台售后 | 工单为 NOT_SUBMITTED_TO_PLATFORM | 初始 NONE | 使用 D 实际枚举，本地完成不表示退款 |
| 知识 | D 不加载；A 范围检索待接入 | 10条 | 用 knowledge.integration.v1.json 交换，不灌全局接口 |

`demoNamespace` 是 C 草案字段；D 使用实体的 `demoData` 标记。不要假设传入 namespace 就有权限操作数据。当前 scope 用于单商家演示，A 工具须从已校验会话构造身份。

## 订单速查

| 简称 / 顾客 | fixture ID | 实际订单号 | 状态与内容 |
|---|---|---|---|
| TB01 / TB1 | demo-order-tb-01 | TB202610100001 | SHIPPED；白色500ml杯59元；10-12 08:30杭州转运 |
| TB02 / TB1 | demo-order-tb-02 | TB202610110002 | PENDING_SHIPMENT；黑色750ml杯69元，无物流 |
| TB03 / TB1 | demo-order-tb-03 | TB202610090003 | DELIVERED；盒2件78元 |
| TB04 / TB1 | demo-order-tb-04 | TB202610080004 | CANCELLED；毛巾，实付0，无物流 |
| TB05 / TB2 | demo-order-tb-05 | TB202610120005 | SHIPPED；杯59元；10-14 16:00转运异常，无ETA |
| TB06 / TB2 | demo-order-tb-06 | TB202610130006 | PENDING_SHIPMENT；盒39元 |
| DY01 / DY1 | demo-order-dy-01 | DY202610100001 | DELIVERED；杯49元；10-13 14:00签收，退货故事 |
| DY02 / DY1 | demo-order-dy-02 | DY202610110002 | SHIPPED；盒35元 |
| DY03 / DY1 | demo-order-dy-03 | DY202610120003 | PENDING_SHIPMENT；毛巾18元 |
| DY04 / DY2 | demo-order-dy-04 | DY202610090004 | DELIVERED；杯+盒84元 |
| DY05 / DY2 | demo-order-dy-05 | DY202610130005 | CANCELLED；盒，实付0 |
| DY06 / DY2 | demo-order-dy-06 | DY202610140006 | SHIPPED；毛巾18元，运输中 |

日期均为2026年固定种子时间。[integration.v1.json](integration.v1.json) 引用 D 种子，校验场景归属、状态与候选，不另复制订单数据。

## 知识交换约定

[knowledge.integration.v1.json](knowledge.integration.v1.json) 标记 `WAITING_FOR_A_SCOPED_IMPORT`，保留稳定知识 ID。MERCHANT 项的 shopId 为 null，SHOP 项必须属于当前商家店铺；productIds 只能引用 D 商品。

共用杯子知识列出实际两种规格；收纳盒资料只用种子已有规格，不从旧草案推断尺寸/材质。6条店铺规则/活动为 C 编制的演示规则，需 A 接入后才能用于真实回答，不能跨店复用。活动不修改历史实付。

A 的加载结果应提供稳定 ID 到真实知识主键的映射、范围、启用/有效期处理。C 再以 AC-13/14 验证，文件自洽不等于检索隔离已实现。

## 尚待联调

- A 会话开通/恢复、快照、人工消息、接待模式、持久上下文及真实执行记录。
- A 实现 AgentTicketGuard，AI 走 createAgentTicket；不能用商家直建接口绕过确认/接管。
- A 统一重置：使旧轮次失效，再协调会话/消息/上下文/记录和 D commerce；D restore 只处理本模块。
- A/D 补合法无订单顾客。每店至少2顾客已有，AC-05 空顾客分支仍缺数据。
- 固定演示时钟或整体平移下单、物流、签收与活动日期，记录策略；目前加载器未实现平移。

10月18日原始设计及计数保留在 dataset.v1.json 和历史结果，不能直接作为当前数据库计数。
