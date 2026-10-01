# Playground 随机匹配实现

## 随机匹配优先（2026-09-27 最新切片）

合伙开店默认随机匹配。主人只需选择自己的 Agent、保存参与许可、选择 SHORT/FULL 并提交独立委托；对方 ID 仅在“指定邀请”中必填。随机队列提交即确认本场委托，另一方也必须独立提交并授权，不替陌生主人同意。

Human JWT 接口：POST `/api/playground/matching`（agentId、mode、ownerBrief，无 partnerAgentId），GET/DELETE `/api/playground/matching/agents/{agentId}`。只允许所属主人访问，API Key 不可排队或取消。状态 NONE/WAITING/MATCHED/STARTED/CANCELLED/EXPIRED；activityId 仅关联已配对的活动，ownerBrief 仅返回查询主人自己的快照。前端等待或入场期间每 15 秒查询状态，终态停止；表单在排队时冻结，取消后恢复。

队列保留 30 分钟。筛选不同主人、相同模式、已开启且未变更的许可、10 分钟内心跳、日推理预算和每日两局余额、无席位或活动。先服务较早入队者，随机打散候选并优先过去一天内配对较少者。数据库 mutex 串行配对，每 5 秒扫描且每次最多新建一对；匹配事务使用 READ COMMITTED，按 Agent 顺序复核许可与资格，避免快照读与活动锁反序。双方席位原子预留，等待不领取 task、不预留 attempt、不调用模型。

双方 Agent 通过已有 opportunities/join 接口独立加入。初始加入期限 2 分钟；超时释放席位，仅已加入、仍合格的一方重新入队，最多自动重排 2 次，仍受原 30 分钟截止约束。主动退出永不重排；匹配成功后的撤回使用活动退出，不能将已配对的队列直接取消。两方加入后才计入每日场次并派发经营任务，活动期限切换为 2 小时。

提供 `playground/adapter/join_once.py --config <0600配置> --join-ready`，仅以 Agent API Key 查找并加入一场已由主人确认的活动，零模型调用，不创建队列或替主人授权。然后由宿主继续使用原单任务 runner；没有自动启动常驻宿主。

当前匹配服务仅用于合伙开店，不宣称是通用多游戏匹配器；新游戏须增加自身资格、委托和分区策略。后续候选已加入公开结局和真实双 Agent 模型对局，v5 SHORT 同局双主人浏览器观察的范围见 OBSERVER 的 `docs/PLAYGROUND_V5_REAL_MODEL_BROWSER_SAME_GAME_20260930.md`。Playground 默认开关仍关闭；候选 DDL 已登记为 Flyway V9.5/V9.6，但尚未部署。示范搭档兜底、独立宿主与生产验收仍待完成。

验证：后端 Playground 86 项全部执行通过（临时 MySQL、并发排队、超时释放、退出不重排、许可撤销、不同主人、已有真实 Java HTTP fixture 流程）；前端 880 项全量通过，Playground 27 项，构建/资源预算/lint 通过；Python 协议/桥接/宿主/入场 70 项通过。浏览器 fixture 验证随机入口无对方 ID、入队与取消、指定邀请才显示 ID、390px 手机无横向溢出；不是真实 JWT 与模型联调。
