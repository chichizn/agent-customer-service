# run13：模型自主生成回复与规则兜底

日期：2026-10-20（Asia/Shanghai）。基线405af06。

工作台日常咨询先以0.1温度分析，再以0.7温度依据当前店铺有效资料和最近10条消息自主组织答案。没有关键词命中也可问候、给通用建议或追问。生成失败或引用无效时使用规则答案。温度可通过 llm.analysis-temperature 和 llm.reply-temperature 配置。生成默认使用 JSON 输出模式，兼容开关为 llm.reply-json-mode。

订单、物流、工单及订单规格查询继续由业务服务处理；售后提交仍需确认。模型请求不持数据库事务，生成结束后重新核对轮次、人工接管状态和资料内容及有效性。引用范围和常见虚构执行结果有校验，但这些检查不能证明所有自然语言事实都正确。

| 检查 | 结果 | 证据 |
|---|---|---|
| 后端测试 | 130通过，0失败/错误/跳过 | automated-checks.json；mvn -f backend/pom.xml -q test |
| 前端测试 | 14通过 | pnpm test:workbench |
| Vue类型检查及演示构建 | 通过 | pnpm build:demo；Node20.18.0低于要求20.19+，有提示但构建成功 |
| 关闭模型HTTP回归 | 57/57通过 | rules-disabled.json |
| 真实模型知识问答 | 11/11检查通过，8次使用生成答案，2次规则兜底 | live-model.json；这是启用JSON模式前的记录 |
| 最终JSON模式自主回复 | 3/3通过 | autonomous-json-mode.json；实际三次耗时约9.6～13.1秒 |
| 数据保留核对 | 7张表原有行均保持；知识及三张工单表完全一致 | data-retention.json |

最终自主回复包括：没有关键词命中时主动询问需求；结合500ml/750ml资料给通勤建议；没有保温测试资料时明确无法确认时长并追问需求。商家知识面板区分模型依据资料生成、模型自主回复或澄清、规则兜底和业务工具处理。

保留首次和中间失败记录：autonomous-first.json、autonomous-final.json、autonomous-diagnostic.json 中有生成降级，诊断观察到模型网关超时和JSON解析异常。随后增加简洁回答要求及JSON输出模式；最终3项通过是有限样本，不能证明供应商长期稳定。restart-unavailable.json 是本机验收启动器重复日志参数导致的连接失败，修正启动参数后服务健康检查为UP，不是模型质量结果。

仅使用127.0.0.1:13307/agent_customer_service_acceptance_20261019；脚本只新增演示会话消息，不重置或提交工单。原始数据库行快照保存在忽略目录backend/target，提交摘要不含行内容和凭据。测试中的重置、工单写入及故障注入使用H2。没有操作日常3306，也未接入真实平台或部署生产环境。

参数参考：OpenAI官方Chat Completions API文档 https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create 。本项目通过兼容网关调用；实际参数支持由网关决定。
