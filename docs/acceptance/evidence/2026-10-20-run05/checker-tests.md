# run05 C 检查器

- 时间：2026-10-20 00:10 Asia/Shanghai。
- 命令：`node --test docs/acceptance/scripts/integration.test.mjs`，Node 24。
- 结果：26 tests，26 pass，0 fail；纯离线检查器回归，不连接 HTTP 服务，也不等价于业务 AC。
- `node demo-data/scripts/check-integration.mjs` 同轮复验通过：2店、5顾客、3商品、12订单、7物流、10条集成知识，errors为空。
