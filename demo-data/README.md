# C 成员数据与联调交付

更新：2026-10-19。当前集成基线在 main `c2fac80` 之后加入 TB3 无订单顾客；需求依据见 [需求 v0.2](../docs/requirements/README.md)。

## 当前使用的数据

真实 commerce 加载器读取 D 的 [demo-data.json](../backend/src/main/resources/commerce/demo-data.json)。C 的 [integration.v1.json](integration.v1.json) 直接引用它，记录身份、数量及三个故事，避免再维护一套不同订单。商家为 `demo-merchant`。

| 代号 | 店铺 / 顾客 ID | 数据与用途 |
|---|---|---|
| TB1 淘宝小林 | demo-shop-tb / demo-customer-tb-1 | tb-01/02 两笔杯子；tb-02 未发货；tb-04 已取消 |
| TB2 淘宝小周 | demo-shop-tb / demo-customer-tb-2 | tb-05 杯子延迟，单匹配及转人工；tb-06 收纳盒未发货 |
| TB3 淘宝小许 | demo-shop-tb / demo-customer-tb-3 | 无订单身份，用于 AC-05 空订单结果；不增加订单 |
| DY1 抖店小林 | demo-shop-dy / demo-customer-dy-1 | dy-01 杯子已签收，退货故事；dy-02/03 |
| DY2 抖店小陈 | demo-shop-dy / demo-customer-dy-2 | dy-04/05/06；dy-06 毛巾运输中，不能用作延迟故事 |

订单 ID 为 `demo-order-` 加表中后缀；真实单号见 [字段与订单对照](FIELD-MAPPING.md)。同名“小林”和平台侧 `buyer-001` 不能作为跨店唯一身份。

种子包含 2 店、5 顾客、3 种订单商品、12 订单、7 物流。TB3 是合法演示顾客但没有订单，用于检查空结果不会泄露其他顾客订单。商品统计来自订单中的不同 productId，**不代表知识商品表已加载 3 行**。

[knowledge.integration.v1.json](knowledge.integration.v1.json) 提供与 D 身份及商品 ID 对齐的 10 条知识材料，修正杯子同时有 500ml / 750ml 的事实。规则与活动为虚构演示店规则；A 已实现范围导入与检索，文件内容在首次打开演示会话时加载。

## 校验与使用

在仓库根目录使用 Node.js 20.19+，新脚本无需第三方依赖：

```powershell
node demo-data/scripts/check-integration.mjs
node --test docs/acceptance/scripts/integration.test.mjs
node docs/acceptance/scripts/commerce-readonly.mjs docs/acceptance/evidence/commerce-latest.json
```

前两条分别检查离线数据、检查器回归（内存替身），通过不代表业务验收通过。第三条在 D 数据加载后仅访问本机10002的身份、订单、物流、工单查询接口；不加载/重置、不建单、不调用模型。退出码 0=PASS、1=响应与契约不符、2=不可达/超时。详见 [验收入口](../docs/acceptance/README.md)。

D 的加载入口为 `POST /api/commerce/demo/load`，补齐缺失数据但不覆盖已有订单/工单；由操作者按 [D 契约](../docs/commerce-contract.md) 在正确数据库执行。C 的只读脚本不会自动写入。

## 历史扩展草案

2026-10-18 的 [dataset.v1.json](dataset.v1.json) 保留了 6 顾客、4 商品、12 订单、8 物流、10 知识的原始扩展设计。它的身份和订单与 D 不同，**不是当前加载器输入**。不要混用其中的 `demo-shop-taobao`、`demo-customer-tb-01` 或 `DEMO-TB-20261018-001`。

```powershell
node demo-data/scripts/validate.mjs
node demo-data/scripts/verify-package.mjs
node demo-data/scripts/load-current-knowledge.mjs
```

这些旧检查仍可离线复验。旧知识加载器只预览原草案4商品、2共用FAQ；`--apply` 会写入全局知识，既不接 D 商品映射，也不支持店铺范围。当前真实集成不要用它导入新知识或店铺规则。10月18日原始数据和证据保持原样。

日期基准为 **2026-10-18 12:00，Asia/Shanghai**，所有日期是固定演示数据。当前加载器不平移日期；后续需 A/D 统一日期策略，不能只延长活动而让签收时间过期。

关联材料：[最新验收记录](../docs/acceptance/RESULTS-2026-10-19.md)、[用例](../docs/acceptance/TEST-CASES.md)、[演示手册](../docs/demo/RUNBOOK.md)、[录像准备](../docs/demo/RECORDING.md)。
