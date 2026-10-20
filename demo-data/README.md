# C 成员数据与联调交付

更新：2026-10-20。当前集成基线已加入 TB3 无订单顾客和演示活动时钟；需求依据见 [需求 v0.2](../docs/requirements/README.md)。

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

[knowledge.integration.v1.json](knowledge.integration.v1.json) 提供与 D 身份及商品 ID 对齐的 **39条**知识材料，包含杯子500ml / 750ml规格、商品保养和使用、问候、支付开票、配送、退换货等常见问题。规则与活动为虚构演示店规则，保养内容是一般建议；实时价格、库存和未提供的检测资料不能视为已核实事实。

每条知识的 `keywords` 提供同义问法，`productKeywords` 提供商品别名；商品资料和活动按当前问题或前文商品过滤。大模型优先从当前商家/店铺的有效知识中选择答案；模型失败或未选出有效依据时，按这些关键词兜底。详细覆盖、试用问题与限制见 [知识与兜底说明](../docs/demo/KNOWLEDGE-AND-FALLBACK.md)。

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

日期基准为 **2026-10-18 12:00，Asia/Shanghai**，种子日期保持不变。后端默认 `DEMO_TIME_MODE=fixed`，仅演示活动有效期使用该基准；可设置 `DEMO_REFERENCE_TIME`（含时区）或 `DEMO_TIME_MODE=system` 后重启。活动起止边界均包含，缺失有效期、未开始和已结束的活动不参与检索。非演示知识始终使用真实时间；消息、工单时间和超时也保持真实时间。退货规则仅供说明，是否受理仍由商家审核，当前不自动判定资格或承诺退款。

JPA 更新表结构后，打开、恢复或继续发送演示消息会补入缺失知识，为旧演示知识补齐类型、有效期及匹配元数据，保留已有文案、启用状态和范围；无需重置订单、会话或工单。旧活动未完成补齐时会被排除，避免无日期活动被当作有效。

关联材料：[最新验收记录](../docs/acceptance/RESULTS-2026-10-20.md)、[用例](../docs/acceptance/TEST-CASES.md)、[演示手册](../docs/demo/RUNBOOK.md)、[录像准备](../docs/demo/RECORDING.md)。
