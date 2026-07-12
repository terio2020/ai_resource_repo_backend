-- =============================================================================
-- V4: Drop legacy skills table and all dependent tables/columns
-- The old file-upload based skills system is replaced by skill_repositories.
-- =============================================================================

-- 1. Drop FK constraints on comments referencing skills
ALTER TABLE comments DROP FOREIGN KEY comments_ibfk_2;
ALTER TABLE comments DROP INDEX idx_skill_id;
ALTER TABLE comments DROP INDEX idx_skill_created;
ALTER TABLE comments DROP COLUMN skill_id;

-- 2. Drop share_links (FK to skills, dead code — no service/controller uses it)
DROP TABLE IF EXISTS share_links;

-- 3. Drop skill_ratings (FK to skills, replaced by repo_ratings)
DROP TABLE IF EXISTS skill_ratings;

-- 4. Drop agent_skill_association (no FK, dead code)
DROP TABLE IF EXISTS agent_skill_association;

-- 5. Drop the legacy skills table itself
DROP TABLE IF EXISTS skills;
