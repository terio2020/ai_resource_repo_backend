# LogicomaNet Git Commit 流程（BE）

跨仓库变更统一使用 `LCM-<编号>`；后端分支使用 `codex/LCM-<编号>-<slug>`，PR 标题使用 `[LCM-<编号>] Summary`。

Commit 使用 Conventional Commits，并包含：

```text
Change: LCM-142
Contract: additive
Tests: mvn test -Dtest=SkillRepositoryServiceImplTest
```

后端应优先提供向后兼容契约，再由 FE 切换，最后由 OBS 固化集成验证。删除或重命名字段必须分两阶段交付。涉及数据库时必须包含 Flyway 迁移、备份和恢复方案。

提交前使用 Java 17 完成测试与构建，并运行 `git diff --check`。合并不等于生产部署；部署必须单独批准并记录应用及数据库回滚点。

本地校验：

```bash
scripts/validate-git-policy.sh branch
scripts/validate-git-policy.sh range origin/main HEAD
```
