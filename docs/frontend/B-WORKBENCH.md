# B 成员前端交付与联调说明

更新：2026-10-19。范围依据 `docs/requirements/README.md` 中 B 的 Vue 分工。

## 当前实现

- 商家三栏工作台：平台/接待模式筛选、顾客搜索、会话切换、历史消息和人工交接摘要。
- 独立顾客模拟页：选择店铺和顾客、恢复/显式新建会话、快捷问题、订单选择和售后确认。
- 订单与物流卡片、本地工单及状态更新、知识引用、执行记录。
- 人工接管、人工回复、交回 AI；切换会话立即清除旧业务视图，输入草稿按会话保存。
- 每 2.5 秒轮询；页面卸载或 KeepAlive 停用时停止同步并取消读取请求；旧读取结果不覆盖当前会话。
- 发送中/失败状态，失败消息保留原 `clientMessageId` 重试，后端必须据此去重。
- 独立模拟存储、同源双标签同步、重置确认；模拟写入使用 Web Locks 串行化。
- 工单按当前店铺与顾客展示，显式新建会话后仍可查看已有工单；完成、取消或接管会话时撤销旧售后确认。

**这是 B 的可独立运行前端版本。默认 `mock`，回复由前端规则模拟，不调用 LLM、Java、MySQL 或平台 API。模拟工单、执行记录和接管校验不等于 A/D 后端已经实现对应需求。**

## D 接口联调状态

真实模式已接入 D 的 commerce 接口：从 `/api/commerce/demo/identities` 读取商家、店铺和顾客；按当前会话身份查询订单、物流和该顾客的全部工单；更新工单时提交 `expectedStatus`、处理说明和操作人。工作台展示后端的订单号、工单号及工单处理历史。首次演示前需调用 `/api/commerce/demo/load` 加载演示数据。

本次已补齐 A 的 `session/snapshot`、`session/open`、人工消息和接待模式更新接口，以及消息编排、范围知识、确认 guard 和统一重置。前端列表显式提交 `workbench: true`，重置提交 `confirmed: true`。接口与后端自动化验证见 [A 后端集成交付](../backend/A-INTEGRATION.md)；真实 MySQL、实际模型和浏览器端到端验收仍待执行。API 请求失败会直接显示错误，不会回退到 mock。

前端模拟数据：两家店、四个顾客、每店六笔订单；三个预置会话。固定数据用于开发和演示，C 的正式演示数据可后续替换。

## 启动与页面入口

使用 Node.js >= 20.19.0 和项目现有 pnpm：

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

同一浏览器内打开：

- 工作台：`http://localhost:20002/#/customer-workbench-preview`
- 顾客入口：`http://localhost:20002/#/customer-simulator`
- 原管理菜单：智能客服 → 客服工作台 / 顾客模拟入口。

当前项目使用 Hash 路由，需要保留 URL 中的 `#`；页面内链接由 Vue Router 自动生成。

两个独立入口复用项目静态路由，便于 B 在 Java 未启动时开发。它们不提供身份认证；切换真实模式后，所有读写权限必须由 A/D 后端校验。

默认本地模拟模式，在 `frontend/.env.local` 中配置 `VITE_CUSTOMER_SERVICE_MODE=api` 并重启 Vite 可切换后端。示例见 `frontend/.env.workbench.example`。API 模式失败会显示错误，**不会自动回退模拟数据**。

模拟数据只保存当前浏览器源的 localStorage 键 `customer-service:b-workbench:v1`，不能跨浏览器/设备同步。Web Locks 不可用时退回单页读改写，不保证多标签同时写入。完整产品的后端持久化、模型调用和工具执行由 A/D 完成。

## 演示步骤

1. 打开工作台，再通过“打开顾客入口”打开同源新标签。
2. 顾客选择淘宝店、林小满，点击恢复最近会话，发送“我的杯子到哪了？”。
3. 点击返回的订单编号，查看物流回复；工作台选中同一顾客查看订单和执行记录。
4. 切到抖店、周雨晴，恢复会话，发送“我想退货，原因是不喜欢这个颜色”，选择订单并确认提交。
5. 工作台选中周雨晴 → 工单 → 更新处理状态 → 处理中，填写说明并保存。顾客发送“刚才的工单进度怎么样了？”。
6. 顾客发送“我要转人工”，工作台筛选等待人工，接管并发送人工回复；顾客端看到同一回复。
7. 交回 AI 后，由新的顾客消息触发回复；工作台刷新仍保留模拟历史。
8. 工作台“重置演示”恢复初始数据；mock 只重置当前浏览器的独立模拟数据，api 经确认请求后端统一重置已标记的演示空间。

## A/D 联调接口

工作台使用新增会话快照接口，保留原聊天 DTO 的兼容路径。下表已由本次后端实现对接；自动化接口验证通过不等于真实环境验收完成。

接口统一 POST，外层沿用 `{ code: 200, msg: "", data: ... }`（也兼容 `code: 0`）。ID 使用字符串，时间为 ISO 8601，金额数值单位元。统一类型见 `src/api/customer-service/model/workbenchModel.ts`。

| 所属 | 路径                                 | 请求                                                           | data                                                           |
| ---- | ------------------------------------ | -------------------------------------------------------------- | -------------------------------------------------------------- |
| D    | `/api/commerce/demo/identities`      | `{}`                                                           | `merchantId`、店铺及各店顾客                                    |
| A    | `/api/chat/session/list`             | `{ workbench: true }`                                          | `WorkbenchSession[]`，包括平台、店铺、顾客、接待模式与最后消息 |
| A    | `/api/chat/session/snapshot`         | `{ sessionId }`                                                | `SessionSnapshot`，当前会话及其业务资料                        |
| A    | `/api/chat/session/open`             | `{ shopId, customerId, forceNew }`                             | `SessionSnapshot`；false 恢复最近 active 会话，true 显式新建   |
| A    | `/api/chat/message/send`             | `{ sessionId, clientMessageId, content, messageType: "text" }` | 处理后的 `SessionSnapshot`，包含真实持久化消息                 |
| A    | `/api/chat/message/human/send`       | `{ sessionId, clientMessageId, content }`                      | `SessionSnapshot`；仅 HUMAN 模式允许                           |
| A    | `/api/chat/session/reception/update` | `{ sessionId, receptionMode: "HUMAN" 或 "AI" }`                | `SessionSnapshot`                                              |
| D    | `/api/commerce/tickets/update`       | `{ merchantId, shopId, ticketId, expectedStatus, status, note, operatorId }` | 更新后的 commerce 工单 |
| A    | `/api/demo/reset`                    | `{ confirmed: true }`                                         | `null`；只恢复指定演示空间                                     |

快照结构：

```typescript
interface SessionSnapshot {
  session: WorkbenchSession;
  messages: WorkbenchMessage[]; // 按持久消息序号升序；role = customer / ai / human / system
  orders: CommerceOrder[]; // 仅当前店铺、当前顾客
  logistics: OrderLogistics[];
  tickets: ServiceTicket[]; // 当前店铺、当前顾客的工单，允许来自其历史会话
  knowledge: KnowledgeReference[]; // 实际引用，含来源和适用范围
  executions: ExecutionRecord[]; // 已完成动作，SUCCESS / FAILED
  selectedOrderId?: string;
  awaitingConfirmation?: boolean;
}
```

顾客点击订单编号会以普通消息发送编号；点击确认提交会发送“确认提交”。A 负责绑定后端持久化上下文、校验具体订单和待办诉求，不能只依赖前端按钮。`awaitingConfirmation` 只用于显示操作入口。

后端必须保证：身份从会话读取并校验、消息/写操作幂等、接管后阻止生成中的迟到回复和写入、工单合法状态流转、失败不返回伪造成功结果。前端轮询和禁用按钮不能代替这些约束。

读取失败显示同步中断并保留最后一次已成功读取的当前会话信息；会话切换时先清空快照。更新失败保留错误和表单。发送结果未知可使用相同标识重试核对；确认提交需要 A 在后端同时校验接待模式与待确认状态。API 超时目前为 60 秒。

工单流转：PENDING_REVIEW → PROCESSING → COMPLETED；PENDING_REVIEW → REJECTED。终态不重开。状态完成只表示本地工单处理完成。

## 验证

```powershell
pnpm test:workbench
pnpm build
```

2026-10-18 本地验证结果：

- 9 个模拟业务测试通过：范围隔离、多订单澄清、工单确认与去重、跨会话工单、旧诉求清理、状态查询、人工回复幂等、接管中止迟到回复/写入、恢复会话、规则差异和独立重置。
- 全项目 `vue-tsc --noEmit` 与 Vite 生产构建通过。
- 本次工作台、顾客入口、模拟层、接口与路由代码的 ESLint，以及工作台 CSS 的 Stylelint 通过。
- 浏览器同源双标签实际验证：多订单选择 → 确认工单 → 商家更新为处理中 → 顾客查询同一工单；转人工 → 商家回复同步 → 人工模式不自动回复 → 交回 AI 后新消息恢复规则回复。页面未出现控制台错误。
- 当前浏览器实际宽度 1072px，已检查两栏与下方业务资料布局；1280px/1440px 的三栏布局尚未在浏览器实测。

构建同时修复了原有代码中的缺失类型导入、游客资料字段、过期 Store 调用、键盘事件类型及标签颜色类型错误。这些旧 Vue 文件已有大量格式 lint 问题，本次仅做必要修复，没有进行整文件格式化，因此不宣称仓库全量 lint 已通过。

测试用 Node.js 24.19.0；工作区 PATH 原有 Node.js 20.18.0 低于项目要求，运行前需切换到满足版本要求的 Node。

这些结果只用于验收 B 前端及模拟层。需求文档的 AC-01～AC-22 仍需在 A/D 真实后端接入后由 C 完整验证，尤其数据库持久化、跨设备同步、实际工具调用与服务端归属校验。
