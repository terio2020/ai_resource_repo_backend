-- =============================================================================
-- V3: Add repo_id to comments table, pointing to skill_repositories
-- =============================================================================

ALTER TABLE comments ADD COLUMN repo_id BIGINT AFTER skill_id;

ALTER TABLE comments ADD CONSTRAINT fk_comments_repo_id
    FOREIGN KEY (repo_id) REFERENCES skill_repositories(id) ON DELETE CASCADE;

ALTER TABLE comments ADD INDEX idx_repo_id (repo_id);
ALTER TABLE comments ADD INDEX idx_repo_created (repo_id, created_at);
