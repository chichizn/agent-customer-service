# Agent Customer Service - 智能电商客服系统

> LLM 大模型 + 知识库检索 + 规则兜底，双引擎架构保证系统永远可用。

当前 Demo 目标与四人分工见 [功能需求说明](docs/requirements/README.md)；B 成员可独立运行的 Vue 工作台、顾客入口及 A/D 联调契约见 [B 前端交付说明](docs/frontend/B-WORKBENCH.md)。

A 的会话、订单/工单编排、人工接管、范围知识及统一重置已补充本地实现，接口、启动步骤与验证范围见 [A 后端集成交付](docs/backend/A-INTEGRATION.md)。完整 MySQL/实际模型业务验收仍待执行。

## 环境要求

| 环境 | 版本 | 必须 |
|------|------|:----:|
| JDK | 17+ | ✅ |
| Maven | 3.8+ | ✅ |
| Node.js | >=20.19.0 | ✅ |
| pnpm | 8+ | ✅ |
| MySQL | 8.0+ | ✅ |
| Redis | 6.0+ | 可选（项目内置降级） |

## 1. 克隆项目

```bash
git clone git@github.com:anjing-le/agent-customer-service.git
cd agent-customer-service
```

## 2. 创建数据库

```sql
CREATE DATABASE agent_customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

表结构由 JPA 自动创建（`ddl-auto: update`），无需手动建表。

## 3. 配置后端

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agent_customer_service?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456        # 改成你的 MySQL 密码

  data:
    redis:
      host: localhost
      port: 6379
      password: 123456                         # 如有密码则填入，无密码留空

# LLM 大模型配置（可选，不配也能正常对话，走规则引擎兜底）
llm:
  enabled: true                          # 改为 true 启用大模型
  api-url: https://api.onerouter.top/v1/chat/completions
  api-key: YOUR_API_KEY                     # 在本地填写，勿提交真实密钥
  model: gpt-4o-mini
```

**API Key 获取方式：**
- OneRouter API Key：https://onerouter.pro

> `application-local.yml` 已被 `.gitignore` 排除，不会上传到 GitHub。

## 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

看到以下日志说明启动成功：

```
========== 智能客服系统启动完成 ==========
端口: 10002
数据库: agent_customer_service
LLM: enabled
```

后端运行在 http://localhost:10002

**命令行验证：**

```bash
curl http://localhost:10002/api/knowledge/overview
```

返回 JSON 数据即成功：

```json
{"code":200,"data":{"productCount":5,"activityCount":3,"faqCount":5,"industryCount":3,"solutionCount":3}}
```

## 5. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端运行在 http://localhost:20002，API 请求配置以 frontend/.env.development 为准。

## 6. 验证

### 方式一：命令行验证

```bash
# 创建会话
curl -X POST http://localhost:10002/api/chat/session/create \
  -H "Content-Type: application/json" \
  -d '{"userId": "test", "userName": "测试用户", "channel": "web"}'

# 用返回的 sessionId 发送消息
curl -X POST http://localhost:10002/api/chat/message/send \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "返回的sessionId", "content": "最近有什么优惠活动吗？"}'
```

### 方式二：前端验证

1. 打开浏览器，登录进入系统
2. 左侧菜单「智能客服」→ **对话中心**
3. 点击「新建会话」，输入"最近有什么优惠活动吗？"
4. 观察 AI 回复 + 右下角推理过程展示

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| 数据库连接失败 `Communications link failure` | 确认 MySQL 已启动，端口 3306，密码正确 |
| 对话只返回固定文案，LLM 没生效 | `llm.enabled` 设为 true 并填入有效 API Key |
| `pnpm: command not found` | 执行 `npm install -g pnpm` |
| Redis 连接失败 | 不影响核心功能，项目内置本地锁降级 |
| 前端 API 404 | 确保后端在 10002 端口运行 |

## 许可证

本项目仅供教学和学习使用。
