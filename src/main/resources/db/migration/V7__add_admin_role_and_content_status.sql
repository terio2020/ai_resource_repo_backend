-- =============================================================================
-- V7: Add ADMIN role index + content moderation status columns
-- =============================================================================

-- 1. Users role query index (V1 verified users already has idx_status/idx_username/idx_email; role has no index)
CREATE INDEX idx_role ON users (role);

-- 2. Content ban status (default VISIBLE, banned=BANNED); V1 verified all three tables lack a status column
ALTER TABLE memories           ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE';
ALTER TABLE skill_repositories ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE';
ALTER TABLE agent_packages     ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE';
