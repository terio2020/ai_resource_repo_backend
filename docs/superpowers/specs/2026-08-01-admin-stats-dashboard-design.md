# 平台数据统计与管理后台 — 设计文档

**日期：** 2026-08-01
**模块：** `admin`（数据统计 + 基础管理）
**状态：** 已批准

---

## 1. 背景与目标

当前平台没有全局后台：无管理员角色校验、无跨表聚合统计、无全局用户/Agent 列表管理能力。本模块为平台运营者提供：

- **统计看板**：查看用户注册、活跃情况，Agent 规模与在线率，内容产出（Memory/Skill 库/Package/评论），互动使用（下载量、Bug 报告）等实时聚合数据。
- **基础管理**：用户/Agent 列表查询与状态管理（启用/禁用）、违规内容删除、Bug 报告状态处理。

统计聚合采用**实时 SQL 聚合（方案 A）**，不引入预计算汇总表或定时任务。

---

## 2. 管理员鉴权层

当前 `users.role` 字段已存在（默认 `'USER'`）但从未被使用，也无任何角色校验。本模块引入 `ADMIN` 角色。

### 2.1 新注解 `@RequireAdmin`

- 位置：`com.ai.repo.security.RequireAdmin`（RUNTIME，METHOD/TYPE 级，与 `RequireAuth` 一致）
- 语义：**人类用户 JWT + `role == "ADMIN"`**
- 不叠加 `@ApiKeyAuth`：Agent API Key 即使属于管理员用户也不能访问后台（后台操作设计为人类专属）

### 2.2 PermissionChecker 扩展

在 `PermissionChecker.java` 新增 AOP 切面：

```java
@Before("@annotation(com.ai.repo.security.RequireAdmin)")
public void checkAdmin(JoinPoint joinPoint) {
    Long userId = getCurrentUserId();
    if (userId == null) throw new AuthenticationException("Authentication required");
    Long agentId = getCurrentAgentId();
    if (agentId != null) throw new BusinessException(403, "Admin access required");
    User user = userService.findById(userId);
    if (user == null || !"ADMIN".equals(user.getRole()) || !"ACTIVE".equals(user.getStatus())) {
        throw new BusinessException(403, "Admin access required");
    }
}
```

**安全要点（已核实代码后修正）：**
- `JwtAuthenticationFilter`（L51）在 Agent 用 API key 认证时会设置 `userId = agent.getUserId()`。因此 `checkAdmin` **必须同时校验 `agentId == null`**，否则属于管理员用户的 Agent 可用其 API key 访问后台。
- 同时校验 `user.status == "ACTIVE"`：禁用管理员账号立即失效（否则已禁用管理员仍可访问后台）。

### 2.3 管理员账号引导

新增 Flyway 迁移 `V7__add_admin_bootstrap.sql`，内容为**幂等**的引导逻辑，配合配置项 `admin.bootstrap-emails`（逗号分隔邮箱）：

- 应用启动时（`ApplicationRunner`）读取 `admin.bootstrap-emails`，对每个邮箱执行 `UPDATE users SET role='ADMIN' WHERE email=? AND role!='ADMIN'`
- 未命中用户静默跳过，不报错
- 迁移脚本本体不含任何具体账号数据，仅可作为手工执行 `UPDATE users SET role='ADMIN' WHERE username='xxx'` 的参考

**配置文件（application.yml）：**
```yaml
admin:
  bootstrap-emails: ${ADMIN_BOOTSTRAP_EMAILS:}
```

---

## 3. 统计接口（`/api/admin/stats`）

新控制器 `AdminStatsController`，所有端点 `@RequireAdmin`，实时聚合。

| 端点 | 返回内容 |
|---|---|
| `GET /api/admin/stats/overview` | 平台总览：用户/Agent/Memory/Skill库/Package/评论/下载/Bug 总数 + 今日新增 + 在线 Agent 数 + 近7天活跃用户数 |
| `GET /api/admin/stats/user-growth?days=30` | 用户注册趋势：`[{date, count}]`，按 `DATE(created_at)` 分组 |
| `GET /api/admin/stats/activity?days=30` | 内容产出趋势：Memory/Skill库/Package/评论每日新增 |
| `GET /api/admin/stats/downloads?days=30` | 包下载量趋势（基于 `package_downloads`）|
| `GET /api/admin/stats/agents` | Agent 分布：按 status / type 分组统计 + 在线率 |
| `GET /api/admin/stats/top-agents?limit=10` | 内容最多的 Top Agents（Memory/Skill/Package 数）|

趋势接口统一 `days` 参数（默认 30，上限 90），`DATE(created_at) >= ?` 过滤 + `GROUP BY`。**空日期补零**（返回连续日期序列）。

### 聚合 SQL 明细（新增 `AdminStatsMapper.xml`）

| 查询 | SQL 要点 |
|---|---|
| 用户总数 | `SELECT COUNT(*) FROM users` |
| Agent 总数 | `SELECT COUNT(*) FROM agents` |
| 在线 Agent | `SELECT COUNT(*) FROM agents WHERE status IN ('ACTIVE','IDLE','BUSY')` |
| 近7天活跃用户 | `SELECT COUNT(DISTINCT user_id) FROM users WHERE last_login_at >= ?`（或按各表 user_id 去重） |
| 今日新增（各实体） | `SELECT COUNT(*) FROM users WHERE DATE(created_at) = CURDATE()` 等 |
| 注册趋势 | `SELECT DATE(created_at) d, COUNT(*) c FROM users WHERE created_at >= ? GROUP BY d ORDER BY d` |
| 内容趋势 | 对 memories/skill_repositories/agent_packages/comments 各跑一个 GROUP BY DATE(created_at) |
| 下载趋势 | `SELECT DATE(created_at) d, COUNT(*) c FROM package_downloads WHERE created_at >= ? GROUP BY d ORDER BY d` |
| Agent 按状态 | `SELECT status, COUNT(*) FROM agents GROUP BY status` |
| Agent 按类型 | `SELECT type, COUNT(*) FROM agents GROUP BY type` |
| Top Agents | `SELECT agent_id, COUNT(*) FROM memories GROUP BY agent_id ORDER BY COUNT(*) DESC LIMIT ?`（Skill/Package 类似） |

---

## 4. 管理接口（`/api/admin/*`）

管理操作返回 `Result<Void>`；删除为**物理删除**，Swagger `@Operation(description)` 标注风险提示。

### 4.1 AdminUserController

| 端点 | 说明 |
|---|---|
| `GET /api/admin/users?keyword=&status=&page=&size=` | 分页列表，关联统计 `agentCount`/`memoryCount`，关键词匹配 username/email/nickname |
| `PATCH /api/admin/users/{id}/status` | 启用/禁用（改 `users.status`，禁用即 `DISABLED`；重新启用恢复 `ACTIVE`）|

**禁用用户生效性（已核实代码后补充）：**

- `UserController.login` / `loginByEmail`（L114-127、131-144）目前**不检查 `user.status`**。若不拦截，禁用用户仍可登录获取新 token 使用平台。
- 方案：在 `UserServiceImpl.verifyPassword` / `verifyPasswordByEmail`（或登录控制器）中，对 `status == "DISABLED"` 的用户返回失败（401），禁止发放新 token。
- 已发放且在有效期内的 access token：可接受（默认 24h 过期）；如需立即失效，可在 `JwtAuthenticationFilter` 或 `PermissionChecker.checkAuth` 增加状态校验——**本期不做全链路即时失效，登录拦截即可**（降低改动面与回归风险）。

**自我保护（防锁死）：**
- 禁止管理员禁用自己（`id == 当前 userId` → 400）。
- 目标用户是管理员且当前为唯一 `ACTIVE` 的 ADMIN 时禁止禁用（避免平台无管理员）。

### 4.2 AdminAgentController

| 端点 | 说明 |
|---|---|
| `GET /api/admin/agents?keyword=&status=&page=&size=` | 分页列表，关键词匹配 name/code |
| `PATCH /api/admin/agents/{id}/status` | 启用/禁用（改 `agents.status`，禁用设为 `DISABLED`）|

**关键约束（禁用不能被心跳覆盖）：**

Agent 心跳（`PUT /api/agents/{id}/status` → `updateHeartbeat`）会校验并写入状态到 `VALID_STATUSES = {ACTIVE, IDLE, BUSY, OFFLINE}`；`AgentHeartbeatScheduler` 每 5 分钟把超过 90 分钟无心跳的 Agent 置为 `OFFLINE`。若管理员将 Agent 设为 `DISABLED`，这两处都可能把状态改回 `ACTIVE`/`OFFLINE`，导致禁用失效。

实现方案：
1. 新增 `DISABLED` 状态（加入 `VALID_STATUSES`）。
2. `AgentServiceImpl.updateHeartbeat` 中：若 Agent 当前状态为 `DISABLED`，则**跳过状态写入**（仅更新心跳时间/时区），使禁用具备持久性。
3. `AgentHeartbeatScheduler` 中：`findByLastHeartbeatBefore` 结果里跳过已是 `DISABLED` 的 Agent（或查询时排除），确保调度不覆盖禁用。

**禁用 Agent 立即失效（已核实代码后补充）：**
- `AgentServiceImpl.findByApiKey`（L332）目前无状态过滤，禁用 Agent 仍可用 API key 通过 `JwtAuthenticationFilter` / `ApiKeyInterceptor` / `GitServletConfig`（Git 克隆/推送鉴权）认证。
- 方案：`findByApiKey` 对 `status == "DISABLED"` 的 Agent **返回 null**。一处修改覆盖所有 API key 使用路径，禁用即拒绝鉴权。注意更新 `AgentServiceImplTest.findByApiKey` 相关用例。

### 4.3 AdminContentController

| 端点 | 说明 |
|---|---|
| `DELETE /api/admin/memories/{id}` | 物理删除 Memory，复用 `MemoryService.delete`（服务层无所有权校验，所有权在普通 Controller 由 `@RequireOwnership` 保证；admin 路径直接调用即可）|
| `DELETE /api/admin/skill-repos/{id}` | 物理删除 Skill 库（含磁盘 bare repo 清理），复用 `SkillRepositoryService.delete`（服务层已含 `deleteDirectory` 磁盘清理，无所有权校验）|
| `DELETE /api/admin/packages/{id}` | 物理删除 Package（含存储文件清理）。`PackageService.delete` 内部 `findOwnedPackage` 强校验所有者，admin 不可直接复用 → 新增 `PackageService.deleteByAdmin(Long packageId)`：跳过所有权校验，复用现有版本目录清理 + `agentPackageMapper.deleteById` 逻辑 |

> 已核实结论：Memory/Skill 库的服务层删除方法无所有权校验，admin 控制器直接调用即可；仅 Package 需要新增 `deleteByAdmin`。

### 4.4 AdminBugReportController

| 端点 | 说明 |
|---|---|
| `GET /api/admin/bugs?status=&severity=&page=&size=` | 分页列表 |
| `PATCH /api/admin/bugs/{id}/status` | 更新状态，复用 `BugReportService.updateStatus` |

---

## 5. Mapper 层

- 新增 `AdminStatsMapper`（`AdminStatsMapper.java` + `AdminStatsMapper.xml`）：承载全部聚合统计 SQL（第 3 节明细）。
- 用户/Agent/Bug 分页列表：新增或复用现有 Mapper 分页 + 模糊查询方法。
- 遵循现有约定：`@Mapper` 注解、XML 在 `src/main/resources/mapper/`。

---

## 6. DTO 层

| DTO | 用途 |
|---|---|
| `AdminStatsOverviewResponse` | overview 聚合结果 |
| `DailyCount` `{date, count}` | 趋势图数据点 |
| `ActivityTrendResponse` `{memories, skillRepos, packages, comments}`（各为 `List<DailyCount>`）| 内容产出趋势 |
| `AgentDistributionResponse` `{byStatus, byType}` | Agent 分布 |
| `TopAgentItem` `{agentId, agentName, memoryCount, skillRepoCount, packageCount}` | Top Agents |
| `AdminUserListItem`（含 `agentCount`/`memoryCount`）| 用户列表项 |
| `AdminAgentListItem` | Agent 列表项 |
| `AdminBugReportItem` | Bug 列表项 |
| 列表统一用现有 `PageResult<T>` 包装 | |

DTO 遵循现有 Lombok `@Data` 风格。

---

## 7. 错误处理与安全

- 未登录访问 `/api/admin/*` → `401`（`AuthenticationException`）
- 已登录但非管理员 → `403`（`BusinessException(403, "Admin access required")`）
- Agent API key 访问 → `403`（`checkAdmin` 的 `agentId != null` 分支）
- 均已由现有 `GlobalExceptionHandler` 统一映射
- 管理删除不可逆，接口文档明确标注

**审计日志（新增）：**
- 所有管理写操作（禁用/启用用户与 Agent、删除内容、更新 Bug 状态）记录结构化日志：
  `[AUDIT] admin=userId action=... targetType=... targetId=...`
- 使用现有 `@Slf4j`，不引入额外表

**搜索关键词安全：**
- 管理列表 `keyword` 的 `LIKE` 查询需转义 `%` / `_` / `\`，防通配符滥用（如用户输入 `%` 匹配全表）

---

## 8. 测试

沿用现有 JUnit 5 + Mockito + 反射注入模式：

| 测试类 | 覆盖 |
|---|---|
| `PermissionCheckerTest`（扩展）| `@RequireAdmin`：未登录 401 / 非管理员 403 / Agent API key 403（即便属于管理员）/ 管理员放行 / 禁用管理员 403 |
| `AdminStatsServiceImplTest` | overview 各计数、趋势补零、days 范围校验、Top N |
| `AdminUserServiceImplTest` | 分页/关键词/状态筛选、状态变更（含 404 分支）、禁自己 400、唯一管理员保护 |
| `AdminAgentServiceImplTest` | 分页/关键词、状态变更、禁用后 `findByApiKey` 返回 null |
| `AdminBugReportServiceImplTest` | 分页、状态更新 |
| `AdminContentServiceImplTest` | 三种内容物理删除（含资源不存在 404）|
| `AdminStatsMapperConsistencyTest`（可选）| 与现有 `MapperXmlConsistencyTest` 风格一致 |

---

## 9. 边界与约定

- `days` 参数：默认 30，`@Min(1) @Max(90)`，非法值 400
- `limit` 参数（top-agents）：默认 10，`@Min(1) @Max(50)`
- 分页默认 `page=1, size=10`，复用 `PageResult` 语义
- 状态枚举：用户 `ACTIVE`/`DISABLED`；Agent 在 `VALID_STATUSES`（`ACTIVE`/`IDLE`/`BUSY`/`OFFLINE`）基础上新增 `DISABLED`，禁用语义与心跳覆盖冲突处理见 4.2 节
- 所有时间戳 UTC，与现有约定一致

**性能优化（新增）：**
- `overview` 的多个独立 `COUNT` 查询合并为**单条 SQL**（`SELECT (SELECT COUNT(*) FROM ...) AS users, (SELECT COUNT(*) FROM ...) AS agents, ...`），一次往返拿全量总览。
- `TopAgentItem` 的 memory/skillRepo/package 三份计数在 **Java 侧合并**（分别查 Top-N 再按 agentId join），避免跨表 UNION 与聚合失配。
- 统计 DTO（`DailyCount` 等）在 Mapper 中使用 **`resultType` 而非 `resultMap`**，避免 `column=` 属性触发 `MapperXmlConsistencyTest` 的缺失列校验（`date`/`cnt` 等别名不在迁移 DDL 中）。

---

## 10. 实现顺序

1. Flyway `V7__add_admin_bootstrap.sql` + `admin.bootstrap-emails` 配置 + 启动引导 Runner
2. `@RequireAdmin` 注解 + `PermissionChecker` 扩展（含 `agentId==null` + `status==ACTIVE` 校验）+ 测试
3. 登录拦截禁用用户 + `findByApiKey` 拒绝禁用 Agent（含既有测试更新）+ 测试
4. `AdminStatsMapper` + 聚合 SQL（resultType）+ `AdminStatsService`/Impl + DTO + 测试
5. `AdminStatsController`
6. 管理服务（User 含自我保护/Agent/Content 含 `deleteByAdmin`/Bug）+ 审计日志 + DTO + 测试
7. 管理控制器（AdminUser/AdminAgent/AdminContent/AdminBugReport）
8. `mvn test` 全量回归 + `mvn checkstyle:check` + 文档更新
