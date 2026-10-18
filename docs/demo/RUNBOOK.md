# 演示运行与操作手册

版本：2026-10-18。当前尚未具备完整业务运行条件，实际状态见 [验收记录](../acceptance/RESULTS-2026-10-18.md)。下面“新版业务演示”是集成后的操作剧本，当前原项目不能按这些步骤完成订单/工单/人工协作。

## 1. 本机启动核对

需要 JDK17+、Maven3.8+、Node20.19+、pnpm8.8+、可连接的 MySQL8；Redis 按项目配置及 A 的降级方案处理。2026-10-18 当前会话只发现 Node/pnpm/Python 命令，未发现 Java/Maven；前后端均未运行。

由 A 先跑通后端。创建数据库示例：

```sql
CREATE DATABASE agent_customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

在根目录复制模板（如果已有本地文件，不覆盖），再在本地文件中填写实际数据库/模型配置；不要把密钥写入演示材料或录像：

```powershell
if (!(Test-Path backend/src/main/resources/application-local.yml)) {
  Copy-Item backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
}
Set-Location backend
mvn spring-boot:run
```

后端以配置端口10002为准。知识总览为 **POST**，根 README 的 GET curl 示例与实际 Controller 不一致，可用：

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:10002/api/knowledge/overview' -ContentType 'application/json' -Body '{}'
```

另开终端，在 `frontend` 目录启动。现有 `.env.development` 未定义 VITE_PORT/VITE_ACCESS_MODE，本说明仅在当前终端显式设置，不修改 B 负责的公共配置。用20002作为本次演示端口：

```powershell
Set-Location frontend
pnpm install
$env:VITE_PORT = '20002'
$env:VITE_ACCESS_MODE = 'frontend'
$env:VITE_VERSION = '0.1.0-demo'
pnpm exec vite --host 127.0.0.1 --port 20002 --strictPort
```

`--strictPort` 防止占用时悄悄换端口。打开终端实际显示的地址；上述环境下为 `http://localhost:20002/auth/login`。游客按钮使用本地游客身份进入智能客服菜单，不能宣称生产认证。新版入口最终 URL 由 B 提供，尚未定义时不要编造 URL。

回到根目录执行：

```powershell
node demo-data/scripts/validate.mjs
node docs/acceptance/scripts/preflight.mjs docs/acceptance/evidence/preflight-latest.json
```

原项目只能加载共用商品/FAQ子集，方式见 [数据说明](../../demo-data/README.md)。完整双店演示须由 A/D 用最终加载器载入 `dataset.v1.json`；记录加载结果、时间平移方式及店铺知识数字主键映射。

## 2. 演示前就绪表

- 1440px 宽桌面主验收，同时检查1280px主要操作；两个独立标签打开顾客模拟入口和商家工作台。
- 后端真实模型开启并经过一轮实际问答；再验证关闭模型也可人工回复和查询。记录模型名，不展示密钥。
- 统一重置一次；12订单、8物流、10条演示知识；运行时会话/工单初始为空。原项目自带知识属于额外非演示数据，不算数量错误。
- 演示日期整体平移或固定演示时钟，抖店退货单签收在有效窗口，活动有效；商家身份始终为 `demo-merchant-01`。
- 不使用缓存的成功截图作为真实操作；显示实际执行记录、实际订单及实际工单号。失败时保留失败状态。

## 3. 身份与订单速查

| 身份 | 店铺 / 完整顾客ID | 订单与用途 |
|---|---|---|
| 淘宝小林 | demo-shop-taobao / demo-customer-tb-01 | TB001杯子运输中；TB002杯子已签收；TB003收纳盒未发货；TB004清洁布已取消 |
| 淘宝小周 | demo-shop-taobao / demo-customer-tb-02 | TB005杯子已签收；TB006收纳盒运输延迟 |
| 淘宝小陈 | demo-shop-taobao / demo-customer-tb-03 | 无订单 |
| 抖店小林 | demo-shop-douyin / demo-customer-dy-01 | DY001杯子运输中；DY002杯子已签收；DY003收纳盒未发货；DY004清洁布已取消 |
| 抖店小周 | demo-shop-douyin / demo-customer-dy-02 | DY005杯子已签收，退货演示；DY006收纳盒延迟，转人工演示 |
| 抖店小陈 | demo-shop-douyin / demo-customer-dy-03 | 无订单 |

TB001 全称 `DEMO-TB-20261018-001`；DY005 全称 `DEMO-DY-20261018-005`。同名顾客及 `buyer_001` 不得跨平台合并。

## 4. 故事一：淘宝多订单查物流（约1分钟）

| 步骤 | 操作/可复制台词 | 现场核对 |
|---|---|---|
| 1 | 顾客选择淘宝店、小林，新建/恢复会话；发送“我的杯子到哪了？” | 商家工作台出现对应会话 |
| 2 | 待系统澄清后选择 `DEMO-TB-20261018-001` | 两笔杯子候选均属于小林；不能自动选错 |
| 3 | 发送“那这个订单现在到哪里了？” | 指向同一订单，展示演示速递和实际运单、杭州转运中心节点 |
| 4 | 发送“保证明天能收到吗？” | 无ETA则说明未知，不承诺 |

讲解：“平台消息为模拟入口产生，订单和物流从本地真实预置数据查询；这里看到的是工具结果。”

## 5. 故事二：抖店退货建单与查询进度（约1分40秒）

| 步骤 | 操作/可复制台词 | 现场核对 |
|---|---|---|
| 1 | 切换抖店、小周；“订单 DEMO-DY-20261018-005 的杯子漏水，想退货。” | 当前顾客和订单正确，读取抖店7天规则 |
| 2 | 对追问补充“杯盖拧紧后仍然漏水”；先不确认 | 此时不能创建工单 |
| 3 | “确认提交退货诉求。” | 生成真实本地工单号，状态PENDING_REVIEW；当场记为TICKET-A |
| 4 | “刚才的申请怎么样了？” | 同一编号、同一状态 |
| 5 | 商家将TICKET-A更新PROCESSING，备注“已联系顾客确认漏水情况” | 更新成功后执行记录真实可查 |
| 6 | 顾客再次问“刚才的申请现在怎么样了？” | 同一编号、最新处理说明，平台售后仍未声称成功 |

讲解：“系统记录了顾客诉求，是否退货/退款需商家和平台处理。本地工单不表示资金已退回。”保留此会话供刷新/重启验证，不现场写死工单号。

## 6. 故事三：超规则赔偿转人工（约1分20秒）

用抖店小周的**另一条明确新建的会话**，避免上一段杯子选单残留：

| 步骤 | 操作/可复制台词 | 现场核对 |
|---|---|---|
| 1 | “订单 DEMO-DY-20261018-006 的收纳盒物流一直没动，必须额外赔我500元，找人工。” | 先确认延迟订单，转WAITING_HUMAN，摘要完整，不承诺赔偿 |
| 2 | 商家筛选等待人工并接管；顾客说“有人吗？现在处理了吗？” | 状态HUMAN，AI不抢答、不新增工单 |
| 3 | 商家回复“已收到，我来核实物流，赔偿需要进一步审核。” | 人工消息自动同步到顾客端 |
| 4 | 商家显式交回AI，再由顾客说“这单物流有更新吗？” | 无历史重放，新消息触发AI，保留DY006上下文 |

讲解：“AI与人工共享同一会话状态，接管后自动处理暂停，人工决定后可交回AI。”不要用固定推理步骤代替真实接待模式和工具记录。

## 7. 失败与演示恢复

| 情况 | 现场处理 |
|---|---|
| 模型不可用 | 展示明确降级标记；只演示已验证的查询/人工流程。不能用固定回复冒充调用成功 |
| 提交后超时 | 先查看实际工单/执行记录，再按原去重键重试，不能新建第二次当成补救 |
| 页面资料串会话 | 记录会话ID和请求，停止相关段落，提交给A/B复验 |
| 订单/知识不存在 | 核对是否完整加载及是否处于正确店铺，不能临时编造订单或成功结果 |
| 需要重置 | 仅用A统一入口，先读影响范围并确认；另一标签按提示重新加载；禁止直接清空表 |
| 现场服务故障 | 若已有经过验收的真实备用录像，明确说明播放的是已录制操作；当前尚无该录像 |

每轮结束执行 AC-22 的两次重置复验，核对非演示资料仍在。签收/活动时间应按同一策略恢复，不能每重置一次随意变更规则。
