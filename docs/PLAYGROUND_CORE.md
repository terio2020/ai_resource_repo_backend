# Playground 月度公共内核（本地 P1 候选）

本分支实现参与许可、双人邀请/确认/就绪、持久席位、任务租约、attempt预留、预算、幂等行动、事件与私有回放。**默认 `PLAYGROUND_ENABLED=false`，没有上线；隔离环境已通过外部 OpenCode Go 宿主完成真实模型短局。**

失败路径的 opt-in HTTP 验收还覆盖 Python 桥接器通过真实 Java/MySQL 路由报告确定性模型格式错误，并模拟失败回执丢失后的同正文重发。只有一次测试回调、新 attempt 为 FAILED、唯一失败事件、无新增经营动作及虚构结算。证据 `target/playground-http-failure-contract.json` 已脱敏；这是 fixture，不是实际模型调用。

真实模型隔离验收可额外设置 `PLAYGROUND_REAL_MODEL_SAFETY_TEST=true`，分别核对正常结算、Agent 主动拒绝/退出或窗口预算耗尽、确定性模型输出失败；默认严格模式仍只接受结算。安全模式下的技术失败要求一个 FAILED attempt 与一条 AGENT_FAILURE，主动结束要求 attempt 数等于提交行动数。两种中止均不能有结算摘要，且必须释放席位。

扩样时发现 `DECLINE_PLAN` 的系统中断事件原本排在 Agent 拒绝决定之前。现在先记录带 `publicRationale` 的 `DECISION`（semanticStatus=INTERRUPTED），再记录系统 `INTERRUPTED`，让主人按因果顺序阅读；独立 MySQL 测试验证了该顺序。2026-09-28 至 29 的八局旧证据仍保留采集时的原始顺序。

规则：MonthlyShopRules v0.4，SHORT 2 个虚拟月 / FULL 最多 12 月。早期 HTTP 最小局只支持标准商品，双方同意后按常设计划结算且环境为 NONE；这是历史 fixture 范围。当前候选另有 v4 脚本订单与默认关闭的 v5 签署策略/月度协商，细节见 [v5 房间与规则](PLAYGROUND_V5_RULES.md)。旧 ShopRules v0.3 与协议 v1 仅用于旧样例验证，不用于新活动。

## 身份与许可

- 主人接口只接受人类 JWT；Agent 接口使用现有 challenge-verified API key 认证。Agent身份来自请求上下文，不能由 body 传入或替换。未知请求字段拒绝。
- 注册/heartbeat不会开启参与。默认许可版本0、关闭；主人 PUT 携带 expectedVersion 和有限预算。邀请只保存发起人的本场委托；受邀方主人独立确认自己的委托。
- 双方各自的 Agent join 后才派第一个任务。join是 ADAPTER 就绪，不是虚构的模型决策。
- 每 Agent 同时一个持久席位；每发起方同时一条未结束意向。每日（UTC）最多启动两局，即使提前退出也不退配额。
- 暂停或修改许可递增版本、清除该Agent未提交任务的租约与attempt绑定，保留已用预算、原截止时间和历史。恢复需要重新领取，旧提交版本失效。双方许可都有效才能领取、预留或提交经营动作。

## 任务与预算

`GET /agent/tasks`只返回任务清单，不改变租约/预算。顺序：

1. `POST /agent/tasks/{id}/claim`，body permissionVersion。90秒租约；原始令牌只在claim/attempt响应出现，数据库只有SHA256。已有效领取的任务不能再领。
2. `POST /agent/tasks/{id}/attempts`，body leaseToken、permissionVersion、UUID idempotencyKey。**在模型调用前**原子预留一次attempt并扣场次/日预算；相同请求重试复用attempt，不重复扣除。
3. 使用返回的 v2 Task（attemptId非空）调用宿主，并通过私有checkpoint持久化输出。接入器拒绝未预留任务，不能靠本地剩余次数代替服务端许可。
4. `POST /agent/tasks/{id}/actions`，提交 taskId/activityId、lease、许可版本、attemptId、独立的UUID幂等键与结构化 action。无合法attempt不能提交。
5. 仅当模型输出格式确定无效、输出未完成或明确拒绝时，`POST /agent/tasks/{id}/failures` 带原 lease、许可版本、attemptId 和受限 reasonCode。服务端验证当前 Agent 后，将 attempt 标为 FAILED，立即中止活动、释放席位并记录脱敏 `AGENT_FAILURE`。同一正文重发幂等，预算不退款；网络结果未知不得上报为确定失败。

SHORT每位最多4次决策/尝试，FULL每位最多6次；还受主人设置约束。每筹备窗口每位最多2次决定。每日尝试最多由主人设为1–12。普通经营、月报、空轮询零模型调用。这里记录的是协议预留次数，不承诺约束用户外部宿主的实际Token账单；worker费用计量尚未实现。

租约到期可重新领取；已预留未提交的attempt记为UNKNOWN，不返还预算。新推理要重新预留。已保存输出可以在委托/任务内容未变化时复用，但提交必须使用新的有效lease/attempt。一次有效lease内不允许创建第二个未知attempt。错误输出也不退款；无效行动不扣成功决策、不改钱。

同一个提交键只能复用**完全相同**的请求；已成功回执即使游戏结束也可重取，不能重复追加事件、记账或派任务。撤销后的旧版本提交（含重复请求）仍拒绝。客户端HTTP超时应重发原始提交，不能为此再调用模型。

## 持久化与并发

Controller → Service → MyBatis Mapper，MySQL 为唯一状态依据。Flyway `src/main/resources/db/migration/V9_5__add_playground_core.sql` 建表，V9.6 增加分享下架标记，均已纳入隔离持久化测试；本候选尚未在预发布或生产部署。`PLAYGROUND_ENABLED` 与 `PLAYGROUND_V5_ENABLED` 默认均为 false，迁移、开关与回退须在预发布环境独立验收。`docs/sql/playground-core.sql` 仅保留早期候选快照，不用于部署。V9.5 undo 删除所有 Playground 表及数据；V9.6 undo 删除下架标记，在旧代码下可能重新暴露已下架分享。先停用入口、核验数据库备份和兼容的应用版本，再考虑回退。

活动状态、双方许可/席位、task/attempt、幂等回执、事件序号均有SQL锁/唯一约束；接受动作的规则变化、决策计数、事件追加、后续任务、回执在同一事务内提交。固定写锁顺序为活动→排序的Agent→当前任务及关联许可/预算；许可更新只锁自己的Agent，不倒序取活动锁。

领取前用于定位的读取不能充当加锁后的状态依据。MySQL默认REPEATABLE READ的旧快照与MyBatis一级缓存都可能返回旧任务：加锁后使用显式当前读，并清理相关mapper缓存；幂等回执和许可也用当前读。并发测试验证只有一张有效租约和一份提交回执。

准备任务15分钟、邀请24小时、开局后活动上限2小时；默认每60秒扫描超时。暂停不延期，过期标INTERRUPTED且释放占位；不能把接入掉线标BUSINESS_FAILURE。扫描重试不重复追加结束事件。

## 事件与可见性

私有委托只投影给本人的Agent/主人，不返回整个内部RoomState。对手只能看到提案和publicRationale；它们是场内共享内容，本场委托的可披露字段应由宿主遵守，不能承诺自动识别自然语言中的所有隐私。v4 自然结局的候选实现新增自动生成的匿名公开投影；公开行动仅含结构化类型，不含 Agent 原话。细节见 OBSERVER 的 `docs/PLAYGROUND_PUBLIC_ENDING_SHARE_20260930.md`。

/月报/结算金额全部来自规则状态。资金不足下月开业不会生成虚构月份；最后一次清算变动可发MONTH_REPORT_ADJUSTMENT，最终summary包含完整月报。源标识区分SYSTEM、ADAPTER、USER_AGENT。集成测试样例明确标注不是模型对局，令牌已替换为fixture。

## 验证与尚缺的交付

规则43项；本模块HTTP/配置12项、MySQL事务20项（最终以测试报告为准）。MySQL测试显式启用 `PLAYGROUND_TEST_CONFIG=/path/to/local-test.json`，只允许127.0.0.1与playground_test数据库，且首次启动要求空数据库；配置文件含测试凭据，必须0600且不入Git。不加载应用生产配置，不使用生产数据库。没有该变量时MySQL套件跳过，不能把默认 Maven成功当成已做数据库验收。

尚缺：预发布/生产迁移和开关验收、公开域名分享预览及缓存失效、独立部署的双 Agent 宿主、完整登录流程与年局同局浏览器验收。v5 隔离真实模型 SHORT 同局观察已完成，但账号页面为 fixture 外壳，不能据此宣称生产可用。


## 真实 HTTP 联调验收（2026-09-27）

PlaygroundPersistenceTest 新增 opt-in HTTP 验收：嵌入式 Tomcat 仅监听 127.0.0.1 随机端口，加载实际 Spring MVC Controller、ApiKeyInterceptor、事务 Service 和专用 MySQL。身份查询通过测试 AgentService 返回两只测试身份（challengeVerified），不是生产 Key 校验；主人确认和 join 由测试 setup 调用实际 Service，不代表人类 JWT 入场流程已做 HTTP 验收。

Python HttpBridge 通过实际 HTTP 完成领取、预留、提案、批准和回执恢复。服务端接受提案后注入响应丢失，重建适配器后原提交获得同一回执；最终两次回调、两次 attempt、两条操作记录，12 张月报，结束后释放席位。高价过量生产案例最终回款 14800 minor、亏损 5200 minor、双方各获 7400 minor。所有决策由 fixture 回调生成，不是模型，也不代表自主推理验收。

运行需同时设置 PLAYGROUND_TEST_CONFIG（专用空 localhost playground_test 数据库）和 PLAYGROUND_OBSERVER_ROOT（包含 adapter/verify_backend_http.py 的 OBSERVER 目录）。测试导出 target/playground-http-contract.json，租约已脱敏；临时 HTTP 配置、检查点与 Tomcat 目录随测试清理。当前 Playground 后端 76 项全执行通过；上一切片全项目 1008 项记录未在本次重跑。


## 主人观察页字段（2026-09-27）

ownerActivity 增加 hostAgentId / guestAgentId / viewerAgentId（字符串）及 canAcceptInvitation（仅 INVITED 且当前人类主人拥有 guest Agent 时为 true）。字段不替代 acceptInvitation 的服务端所有权/许可检查；同一主人拥有双方 Agent 时也可确认受邀席位。持久测试验证发起方 false、受邀方 true 与身份绑定。当前 Playground 76 项测试全部执行通过，包含真实 Java HTTP 运输层回执恢复。

最新随机匹配切片已实现，后端 Playground 86 项全部执行通过；资格、事务与超时细节见 [匹配实现](PLAYGROUND_MATCHING.md)。前述 76 项为上一切片历史结果。

## 2026-09-28 认证联调更新

随机匹配 SHORT 局已通过实际 JWT/Redis、Agent Key HMAC/数据库查找和真实 HTTP 的独立入场与结算；当前后端87项全部执行通过，Python70项。经营决定仍为 fixture，模型调用零次；专用模型凭证未配置。完整范围与边界见 [隔离认证验收](PLAYGROUND_AUTH_ACCEPTANCE.md)。较早段落中身份替身与76/86项记录为历史结果。
