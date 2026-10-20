# Agent Customer Service - 智能电商客服系统

> Vue + Java 电商客服演示项目：LLM 意图分析、范围知识检索和业务规则；模型异常时查询降级，无法安全执行的写诉求转人工。

当前 Demo 目标与四人分工见 [功能需求说明](docs/requirements/README.md)；B 成员可独立运行的 Vue 工作台、顾客入口及 A/D 联调契约见 [B 前端交付说明](docs/frontend/B-WORKBENCH.md)。

A 的会话、订单/工单编排、人工接管、范围知识及统一重置已补充本地实现，接口见 [A 后端集成交付](docs/backend/A-INTEGRATION.md)。2026-10-20 完成真实模型/隔离MySQL的三故事双端操作、刷新恢复、连续UI重置与非演示数据保留；另有17组可控并发/故障HTTP检查通过。后端83项、前端14项、C检查器26项及Vue类型/生产构建通过；完整22项AC仍有部分组合及证据未补齐，模型存在偶发超时，详见[最新验收记录](docs/acceptance/RESULTS-2026-10-20.md)。已完成内容及需要项目负责人提供的账号、业务资料和部署资源见[下一步清单](docs/demo/NEXT-STEPS.md)。

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
git clone git@github.com:chichizn/agent-customer-service.git
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

# 本地单实例，无 Redis 时使用本地锁
app:
  features:
    redis:
      enabled: false
    distributed-lock:
      enabled: true
      provider: local

management:
  health:
    redis:
      enabled: false

# LLM 大模型配置（可选，不配也能正常对话，走规则引擎兜底）
llm:
  enabled: true                          # 改为 true 启用大模型
  api-url: https://api.onerouter.top/v1/chat/completions
  api-key: YOUR_API_KEY                     # 在本地填写，勿提交真实密钥
  model: gpt-4o-mini
  connect-timeout-ms: 3000                # 1～5000 毫秒
  read-timeout-ms: 8000                   # 1～20000 毫秒；较慢网关可设 20000
```

**API Key 获取方式：**
- OneRouter API Key：https://onerouter.pro

> `application-local.yml` 已被 `.gitignore` 排除，不会上传到 GitHub。

使用服务商提供的完整 Chat Completions 地址及已开通模型名。超时也可通过 `LLM_CONNECT_TIMEOUT_MS`、`LLM_READ_TIMEOUT_MS` 配置（本地 YAML 同名配置优先）；0或越界会拒绝启动。提高超时只延长等待，不保证网关成功；模型分析失败或返回非法意图时，不自动创建售后工单。

演示活动默认按 `2026-10-18T12:00:00+08:00` 判断有效期，工作台和顾客端会显示这一基准。可在启动后端的终端设置 `DEMO_TIME_MODE=system` 使用系统时间，或设置 `DEMO_REFERENCE_TIME` 修改固定基准（必须含时区，例如 `2026-11-01T12:00:00+08:00`）。种子日期不会自动平移；消息、工单和请求超时仍使用真实时间，非演示知识始终按系统时间过滤。售后仍由商家审核，当前不自动判定退货资格。

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

工作台默认使用前端模拟。联调真实后端时，在 `frontend/.env.local` 配置 `VITE_CUSTOMER_SERVICE_MODE=api`、`VITE_API_URL=/`、`VITE_API_PROXY_URL=http://127.0.0.1:10002`，重启前端。示例见 `frontend/.env.workbench.example`。两个入口为 `/#/customer-workbench-preview`（商家）、`/#/customer-simulator`（顾客），首次使用须先加载演示种子，步骤见 [A 后端集成交付](docs/backend/A-INTEGRATION.md)。

`npm run build` 会执行 Vue 类型检查并生成静态文件；真实部署仍须显式配置 API 模式、`VITE_API_URL` 及同源 `/api` 反向代理。Vite 开发代理不适用于生产静态服务。

本地静态API演示可在 `frontend` 目录运行 `pnpm run build:demo`、`pnpm run serve:demo`，访问 `http://127.0.0.1:20003/#/customer-workbench-preview`。该模式显式构建真实API版本，并将同源 `/api` 转发至本地10002；仅监听回环地址。详见[静态演示步骤](docs/demo/RUNBOOK.md)。

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
| 本地没有 Redis | 按上方关闭 Redis 并选择 local 锁；多实例部署启用 Redis、redisson 和 Redis 健康检查 |
| 前端 API 404 | 确保后端在 10002 端口运行 |

## 许可证

本项目仅供教学和学习使用。
