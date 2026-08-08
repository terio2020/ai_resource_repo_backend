-- =============================================================================
-- V7__add_admin_role_and_content_status-undo.sql — rollback V7__add_admin_role_and_content_status
-- Corresponding migration: ../migration/V7__add_admin_role_and_content_status.sql
-- =============================================================================

-- Rollback add_admin_role_and_content_status:
ALTER TABLE agent_packages     DROP COLUMN status;
ALTER TABLE skill_repositories DROP COLUMN status;
ALTER TABLE memories           DROP COLUMN status;
DROP INDEX idx_role ON users;
