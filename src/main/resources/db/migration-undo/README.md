-- =============================================================================
-- V*-undo.sql — Flyway 迁移回滚模板 (P22)
-- 命名规则: 与 V{n}__add_xxx.sql 对应, 放 db/migration-undo/ 目录
-- 示例: V6__add_bug_reports.sql → V6__add_bug_reports-undo.sql
-- =============================================================================
-- 注意: 每次回滚 undo 前必须确认 prod 当前版本能安全回退
-- 执行: 通过 deploy.sh --self-audit 或手动 deploy.sh --rollback <version>

-- 示例: 撤销 bug_reports 表 (V6)
-- DROP TABLE IF EXISTS bug_reports;

-- 示例: 撤销新增列
-- ALTER TABLE agents DROP COLUMN IF EXISTS new_column;

-- 示例: 撤销新增索引
-- DROP INDEX IF EXISTS idx_xxx ON agents;