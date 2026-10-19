# run09：旧聊天上下文持久化与全量回归

时间：2026-10-20 04:35～04:37（Asia/Shanghai）

## 代码变更

- `ChatSession` 增加 `selectedProductsJson`、`selectedActivitiesJson`、`userProfileJson` 三个 TEXT 字段。
- `ChatService.updateContext` 将上下文序列化写入会话记录；null 字段保留原值，空列表或空对象可以清空对应值。
- `ChatService.getContext` 和会话详情从数据库反序列化上下文。
- 新增 `ChatContextPersistenceTest`，覆盖持久化重载、会话详情返回、部分更新和清空选择。

## 验证命令

| 检查 | 结果 |
|---|---|
| `cd backend; mvn -o -B -Dtest=ChatContextPersistenceTest test` | PASS，2项，0失败、0错误、0跳过 |
| `cd backend; mvn -o -B test` | PASS，64项，0失败、0错误、0跳过 |
| `cd frontend; npm run test:workbench` | PASS，14项，0失败 |
| `cd frontend; npm run build` | PASS，Vue 类型检查及 Vite 生产构建完成 |
| `git diff --check` | PASS |

前端构建提示当前 Node.js 为 20.18.0，而 Vite 建议 20.19+ 或 22.12+；本次类型检查和生产构建仍然成功。完整 AC-01～AC-22 仍按验收记录保持原状态，不能由本轮自动化回归折算为全部通过。
