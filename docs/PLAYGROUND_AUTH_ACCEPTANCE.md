# Playground 隔离认证联调验收（2026-09-28）

已通过随机匹配 SHORT 局的实际 HTTP 全链路：两个主人独立使用签名 JWT 保存许可并提交委托，两个 Agent 以各自 API Key 查询 opportunities、独立加入、提交提案/批准，规则结算两个虚拟月，最后释放席位。无任何生产身份或提供商凭证。

## 实际覆盖

- 嵌入式 Tomcat localhost 随机端口，实际 Controller、事务 Service、MyBatis 与新建 MySQL playground_test 数据库。
- 实际 JwtAuthenticationFilter / JwtProvider 校验签名与 Redis 中的令牌绑定；JWT 由测试中的实际 JwtProvider 签发。Redis 仅连接 localhost，开始前必须为空。
- Agent 鉴权调用实际 AgentServiceImpl 的 findByApiKey/findById、ApiKeyHashUtil 和真实 Mapper 查询。测试仅装配身份查找所需依赖，并通过接口代理暴露这两个实际方法，未启动其余平台服务；不再用 Mockito 替代身份返回。
- Owner JWT 不能调用 Agent join；Agent Key 不能读取主人队列；主人不能查询其他主人的队列；匿名请求在认证请求之前和之后均拒绝。
- 同一隔离 HTTP 驱动还创建可公开分享的 v4 结局，并实际装配 `PermissionChecker` 验收管理员下架：匿名 401；普通主人、Agent Key、停用管理员 403；活跃管理员 200；公开 JSON/HTML 从 200 变成 404，主人再次取链得到 410。身份来自真实签名 JWT、Agent Key 哈希查询与 MySQL 用户状态，Agent 决策仍为 fixture。
- `SecurityConfigTest` 使用实际 Spring Security 过滤链，确认仅结局 JSON 与 HTML 落地页两个 GET 路由匿名放行；同前缀额外路径、主人取链接口及管理 POST 保持需认证。另以 `PLAYGROUND_SECURITY_CHAIN_TEST=true` 把实际 `SecurityConfig` 和 `DelegatingFilterProxy` 接入上述隔离 Tomcat/MySQL/Redis 驱动，完成同一请求链的 JWT、Agent Key、管理员下架及公开链接失效验收；定向测试 1/1 通过。未启用该开关的原认证驱动也重新通过 1/1。
- 第一位主人入队返回 WAITING且无 activityId；第二位主人提交后才 MATCHED，各自只能看到自己的委托快照。加入前没有推理任务，两只 Agent 均加入后 PLANNING。
- Python join_ready 以两份独立 Agent Key 经真实 HTTP 入场。双方经营决策仍为 fixture 回调，实际模型调用为零。
- 提案提交后模拟响应丢失，重建 HttpBridge 后重发原提交获得回执；最终只有两次决策回调、两次 attempt和两条行动，未重复推理或记账。
- 证据导出前替换 leaseToken，并断言不包含任何本轮 Agent Key或 JWT。临时配置0600，结束后删除；数据库和Redis容器只作本轮隔离测试。

## 复现

在新建的 localhost MySQL playground_test 与空 Redis 上运行；配置文件必须0600且不得提交。配置字段为 url、password、redisPort，MySQL url严格限制127.0.0.1与playground_test，首次执行拒绝已有表。

```sh
PLAYGROUND_TEST_CONFIG=/本地私有目录/test-config.json \
PLAYGROUND_OBSERVER_ROOT=/Users/ifish/development/LOGICOMA-OBSERVER \
PLAYGROUND_AUTH_TEST=true \
mvn -q '-Dtest=com.ai.repo.playground.**.*Test' test
```

后端 Playground 87 项全部执行通过，Python协议/宿主/适配器70项通过。本轮未修改前端，前端880项与构建/手机检查为上一切片证据，不作为本轮重新执行结果。

脱敏契约位于后端 target/playground-auth-http-contract.json及 OBSERVER playground/fixtures/authenticated-short-game.json；原先 FULL fixture 运输层测试也继续通过。

## 尚待验收

这不代表真实人类从登录页面完成前后端操作，也未按生产环境配置部署；上述完整过滤链只在隔离 Tomcat 中装配。两个独立模型宿主的自主经营及生产配置兼容性仍待验证。不得把 fixture 提案/批准算成真实 Agent 对局，不得据此开启生产 Playground。

后续 v5 隔离样本已让两只真实模型 Agent 完成同一 SHORT 局，并让两位主人以签名 JWT 在桌面/手机浏览器观察该局；详见 OBSERVER 的 `docs/PLAYGROUND_V5_REAL_MODEL_BROWSER_SAME_GAME_20260930.md`。该样本的账号页面由 fixture 提供，两只桥接实例同处测试进程。仍需两个独立部署宿主、年局同局浏览器、完整登录链路，并在预发布环境核对真实配置、反向代理、公开域名与社交平台缓存失效。上述早期认证 fixture 不应单独计为真实模型验收。
