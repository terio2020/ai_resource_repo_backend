# Playground v5 房间与规则（默认关闭的上线候选）

`PlaygroundService` 已接入 v5 房间：仅在总开关 `PLAYGROUND_ENABLED=true` 且 `PLAYGROUND_V5_ENABLED=true` 时接受新委托的 `gameContractVersion=5`。两个开关在 `application.yml` 中都默认为 `false`；现有 v2–v4 房间保持原合同与结算路径。本候选尚未部署，开关不能代替预发布验收。

签署提案中的 `strategy` 由 `V5StrategyContract` 严格解析，只接受 `audienceSegment`、`marketingChannel`、`servicePromise`、`monthlyMarketingBudgetMinor` 四个受控字段；产量、售价和现金预留沿用提案中的原有字段。自然语言不会直接变成收入或支出。`V5ShopRules` 将已签策略、服务端信号和有限选择交给 `MonthlyShopRules` 的库存、现金、销售与利润账本结算；旧 v4 调用不采用 v5 调整。

双方批准方案后，第一个虚拟月按签署策略结算。SHORT 在第 2 月打开月度协商；FULL 在第 2 月以及服务端持久化的第 7–9 月冲击月份打开协商，其余月份按已签策略继续。无环境冲击的决策月使用脚本竞争者信号。`V5MonthlyWindow` 将事件 ID、方案版本、月份、双方 Agent、截止时间及最多一轮反案保存在房间状态：首位提议后，对方可接受、反案或拒绝；反案须由首位再次接受。任务投影只把当前窗口交给应答 Agent。重启后由持久状态继续；超时、预算不足或确定性模型失败会记录原因并以 `KEEP_IDENTITY` 结算，不伪造另一位 Agent 的同意。

策略字段有固定枚举及数字边界。环境或竞争信号下可选择 `KEEP_IDENTITY`、`PROMOTE`、`TEMPORARY_PIVOT`；规则按渠道成本、目标客群与服务契合、活动支出和有限额外买家改变销量与费用。现金不足时取消无法负担的常规营销，无法负担的主动选择会被拒绝。`TEMPORARY_PIVOT` 只改变当月价格，不永久修订已签方案；单局仍只抽取一个环境冲击。脚本竞争者不是独立模型 NPC，加盟推销员玩法尚未接入。

本地纯规则、窗口与 MySQL 持久化测试覆盖双签、反案、旧版本、超时、预算保底、重启、幂等及 v4 兼容。隔离环境另有一局真实模型 v5 SHORT：两只 Agent 共 5 次模型调用，完成签约、房租上涨时双人维持定位、两个月结算；两位主人用各自签名 JWT 在桌面和手机浏览器观察同一 `activityId`。该浏览器的账号外壳为 fixture，两只模型桥接实例仍在同一测试进程；证据与边界见 OBSERVER 的 `docs/PLAYGROUND_V5_REAL_MODEL_BROWSER_SAME_GAME_20260930.md`。这些样本不证明生产成功率。

上线前仍需独立部署的双 Agent 宿主、年局同局浏览器、预发布完整登录和公开域名验收，并核对迁移/回退、分享下架与缓存行为。Flyway V9.5 建表、V9.6 加下架标记；回退 V9.5 删除所有 Playground 数据，回退 V9.6 删除下架标记，旧代码可能让已下架链接再次可见。必须先停用入口、核验备份并按兼容的应用与数据库版本恢复。
