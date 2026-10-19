# run03 环境与构建记录

日期：2026-10-19，Asia/Shanghai。工作区基线 `main` / `c2fac80`，包含尚未提交的 TB3 集成改动。

## 后端

早期执行 `mvn -o -B -Dtest=WorkbenchApplicationHttpTest test` 时，主源码编译成功而 testCompile 无法解析项目类型。后续本机验证已解除该阻塞，HTTP 测试通过；最终再执行 `mvn -o -B test`，于23:18:01 +08:00退出0，37项全部通过，0失败、0错误、0跳过，`BUILD SUCCESS`。各测试类计数及验证边界见 [backend-tests.md](backend-tests.md)。

HTTP 测试排除 Redisson starter 自动配置并注入 `RedissonClient` 替身，启动完整 Spring Boot 应用和随机 HTTP 端口，使用独立 H2 内存库、关闭 LLM。测试覆盖真实应用路由及 JSON 响应，不能作为真实 MySQL 或实际模型验收。

MySQL/Redis 真实服务未连接，未对本地业务数据库执行建表、加载、工单写入或 reset。run02记录的MySQL登录1045仍未解决，完整AC仍为0 PASS、22 BLOCKED。

## 前端

Node 24.19.0 下 `vue-tsc --noEmit` 退出码为0。沙箱内 Vite 构建曾因 esbuild `Cannot read directory "../..": Access is denied` 无法解析配置；tsx 正常入口也曾因 Windows 用户信息查询受限报 `uv_os_get_passwd` 错误。这些受限调用不是当前代码测试失败的结论。

最终在允许读取用户信息及项目祖先目录的本机验证方式下，以下命令依次完成，整个命令退出码为0：

```powershell
node --import tsx --test scripts/workbench.test.ts scripts/commerceAdapter.test.ts
node node_modules/vue-tsc/bin/vue-tsc.js --noEmit
node node_modules/vite/bin/vite.js build
```

13项前端测试全部通过，0失败、0跳过；类型检查通过；Vite 7.1.7构建转换2949个模块，13.00秒完成并生成 `dist/`。测试耗时15978.2818毫秒。早期临时Node预加载脚本和编译探针已删除，未修改项目运行配置。
