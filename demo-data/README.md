# C 成员演示数据交付

交付日期：2026-10-18。业务需求依据：[`docs/requirements/README.md`](../docs/requirements/README.md)。

## 数据包内容

[`dataset.v1.json`](dataset.v1.json) 是完整、可解析的演示交换数据，包含 1 个商家、2 家店铺、6 位顾客、4 种商品、12 笔订单、8 份物流、10 条知识。会话、消息、工单和执行记录初始为空，必须由真实操作产生。

| 平台 | 店铺 ID | 顾客 ID 后缀 | 用途 |
|---|---|---|---|
| 淘宝 | `demo-shop-taobao` | `tb-01` 小林 | 多笔保温杯订单，测试先选单再查物流 |
| 淘宝 | 同上 | `tb-02` 小周 | 仅一笔杯子订单，另有延迟的收纳盒订单 |
| 淘宝 | 同上 | `tb-03` 小陈 | 无订单 |
| 抖店 | `demo-shop-douyin` | `dy-01` 小林 | 同名顾客、多笔订单及跨店隔离 |
| 抖店 | 同上 | `dy-02` 小周 | 杯子退货建单与进度查询、收纳盒异常赔偿转人工 |
| 抖店 | 同上 | `dy-03` 小陈 | 无订单 |

完整顾客 ID 为 `demo-customer-` 加表中后缀。两平台刻意复用 `buyer_001` 等平台侧标识及展示姓名；这些字段不能作为跨店查单的唯一身份。

两店都有同名“保温杯”和“收纳盒”；共用商品事实，售后规则及活动独立。淘宝退货申请窗口为 15 天，抖店为 7 天。这些都是虚构演示店规则，不代表平台政策。

## 校验与使用

在仓库根目录，用 Node.js 20.19+ 执行，无第三方依赖：

```powershell
node demo-data/scripts/validate.mjs
# 重复执行离线数据负例、加载器防重/冲突校验，并保存证据
node demo-data/scripts/verify-package.mjs docs/acceptance/evidence/deliverable-checks-latest.json
```

校验订单归属、金额、商品引用、物流时序、双平台数据量、多订单/无订单案例和店铺规则差异。退出码 0 表示数据一致性校验通过，不代表系统功能验收通过。

### 当前原项目可加载的子集

现有接口只有商品/FAQ 等全局知识，缺少店铺归属。脚本仅映射 4 个商品和 2 条共用 FAQ，**不会导入两店不同的规则或活动**，防止把店铺专属政策混入全局检索。

```powershell
# 预览请求，不联网、不写库
node demo-data/scripts/load-current-knowledge.mjs

# 后端启动后，显式执行本地写入
node demo-data/scripts/load-current-knowledge.mjs --apply
```

目标固定为本机 `http://localhost:10002`。通过现有 CRUD 接口创建，自动取得数字主键；商品稳定编码为 `C-DEMO-*`，FAQ 标题带 `[C-DEMO-V1]`。再次执行会跳过相同记录；遇重复编码或内容冲突即停止，不覆盖已有数据。先完成全部冲突检查，再开始写入。

创建状态沿用当前接口默认值（商品/FAQ 为草稿），脚本不擅自发布。当前 ChatService 未按发布状态过滤，不能把“草稿”理解为已经被检索排除。需要展示上架/启用状态时由 A/D 按实际接口处理。

网络超时可能发生在服务端写入之后；先检查记录，再重跑。该脚本不具备跨进程并发去重能力，**只由一位操作者串行运行**。它不提供删除或重置功能，不使用当前返回模拟结果的 `/import` 接口。

### 新版完整数据接入

完整包的 `contractStatus` 为 `DRAFT_FOR_A_D_MAPPING`。仓库尚无 A/D 的字段模板、commerce 实体和导入接口，不能把本包当成已落库的数据。A/D 按 [`FIELD-MAPPING.md`](FIELD-MAPPING.md) 映射到最终 DTO/实体，完成统一加载和重置；C 负责复验数据和演示。

日期基准为 **2026-10-18 12:00:00，Asia/Shanghai**。后续演示必须整体平移日期或使用后端演示时钟，详见字段说明；不能仅改活动结束时间而让签收时间过期。

## 关联交付物

- [`验收用例`](../docs/acceptance/TEST-CASES.md)：AC-01～AC-22 的数据、操作与判定标准。
- [`当前验收记录`](../docs/acceptance/RESULTS-2026-10-18.md)：真实执行结果与阻塞。
- [`演示手册`](../docs/demo/RUNBOOK.md)：三个演示故事和启动步骤。
- [`录像脚本`](../docs/demo/RECORDING.md)：待系统可用时录制的镜头与核对项。
