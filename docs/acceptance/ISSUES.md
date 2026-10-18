# 问题与依赖清单

检查日期：2026-10-18。源码基线 `c86f6645406306b41e0f7bd9bba6326ff1a40004`。下表除 ENV-01 外均为**静态检查发现/需求缺口，尚未运行复现**。责任人是按需求分工建议，未发送给其他成员。

| 编号 | 优先级 / 建议负责 | 证据及影响 | 集成后最小复验 |
|---|---|---|---|
| ENV-01 | 阻塞 / A 配合环境 | [预检证据](evidence/preflight-2026-10-18.json)：Java/Maven 不在 PATH；本机10002、20002、3100、5173均拒绝连接；前端未安装 node_modules。MySQL 命令未发现不能单独证明 MySQL 服务不存在 | 配齐路径及数据库后启动前后端，重跑预检；再实际创建会话、发送消息 |
| DATA-01 | 阻塞 / A、D | 尚无 commerce 模块、A/D 字段模板、完整数据加载/统一重置契约。C 的 fixture 为待映射格式 | 加载完整包两次，核对归属与计数；按 AC-22 验证非演示数据保留 |
| GAP-01 | P0 / A、B、D | 当前 [ChatSession](../../backend/src/main/java/com/anjing/module/chat/entity/ChatSession.java) 没有商家、店铺、顾客和接待模式字段；[API](../../backend/src/main/java/com/anjing/model/constants/ApiConstants.java) 未见订单/物流/工单完整端点 | AC-01～12：实际查询/写入后核对业务记录，完成同店/跨店隔离 |
| GAP-02 | P0 / B、A | [ChatInterface.vue](../../frontend/src/views/customer-service/chat/components/ChatInterface.vue) 第56～83行：挂载即创建 `user_demo` 会话；创建失败改用 local_ ID；没有历史切换和身份入口 | AC-02/21：刷新恢复同一会话；失败明确提示，不能伪造可用服务端会话 |
| GAP-03 | P0 / A | [ChatService.java](../../backend/src/main/java/com/anjing/module/chat/ChatService.java) 第287～295行：更新上下文仅日志、读取返回空对象 | AC-04/11/21：选择订单、待确认诉求持久化，刷新/重启仍正确 |
| GAP-04 | P0 / A、D | 同文件第377/404/417行全局 findAll 检索；活动触发时读取所有活动；现有知识实体无店铺范围，未见状态/有效期统一过滤 | AC-06/07/13/14：相似身份与同名商品不串店；禁用/过期知识按最终契约处理 |
| GAP-05 | P0 / A、B | 当前主流程未见 WAITING_HUMAN/HUMAN 切换、人工消息接口、处理中接管版本校验；物流卡片类型不等于工具已执行 | AC-15～18：接管后连续输入不触发AI或写工单，迟到回复被阻止 |
| GAP-06 | P0 / A | [LlmService.java](../../backend/src/main/java/com/anjing/module/chat/LlmService.java) 第33行直接 new RestTemplate，未见请求超时；前端 [http/index.ts](../../frontend/src/utils/http/index.ts) 第25行15秒超时；单次聊天有两次串行LLM请求 | AC-19：明确后端总耗时界限、降级标记；前端超时后不伪称服务端已取消 |
| GAP-07 | P0 / A、D、B | [ChatService.java](../../backend/src/main/java/com/anjing/module/chat/ChatService.java) 第545行起固定构造步骤；第237/555行以分析调用是否成功决定“生成回复”引擎标签，而回复调用可能另行失败；未见持久工具执行记录 | AC-19/20/21：展示实际动作和实际回复引擎，失败不标成功，刷新后记录可查 |
| DOC-01 | P1 / C 记录，A/B 修复公共配置 | 根 README 写前端3100、需求写20002；实际 [vite.config.ts](../../frontend/vite.config.ts) 读取 VITE_PORT，但已跟踪 `.env.development` 未设置；VITE_ACCESS_MODE 同样未设置，游客菜单可能走后端菜单路径 | 用 [演示手册](../demo/RUNBOOK.md) 的命令显式指定 frontend 模式和20002；检查实际终端端口，再更新主说明 |
| OUT-01 | 本版不验收 / A、B 评估 | [KnowledgeService.java](../../backend/src/main/java/com/anjing/module/knowledge/KnowledgeService.java) 第717行起向量化仅标记状态，导入/导出返回固定结果；不算真实业务能力 | 演示不展示为已完成；基线数据使用真实 CRUD 脚本 |
| OUT-02 | P1 / A、B | [SceneService.java](../../backend/src/main/java/com/anjing/module/scene/SceneService.java) 第286～303行渲染模板但未把内容传给模型；[IntentConfig.vue](../../frontend/src/views/customer-service/scene/tabs/IntentConfig.vue) 第58行仅提示切换成功 | 本版不演示为动态规则引擎；若纳入范围，验证实际保存、实际注入及刷新恢复 |

优先修复 ENV-01、DATA-01 和 GAP-01～07，才能执行 P0 业务验收。C 不在本次任务中修改这些成员负责的业务代码或公共配置。
