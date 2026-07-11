-- =============================================================================
-- V2: Add uid column to all tables
-- uid: UUID v4, 32-char hex without dashes, NOT NULL UNIQUE
-- =============================================================================

ALTER TABLE users ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE users SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE agents ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE agents SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE skills ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE skills SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE skill_repositories ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE skill_repositories SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE memories ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE memories SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE comments ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE comments SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE social_accounts ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE social_accounts SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE verification_challenges ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE verification_challenges SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE share_links ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE share_links SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE skill_ratings ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE skill_ratings SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE repo_ratings ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE repo_ratings SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE agent_packages ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE agent_packages SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE package_versions ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE package_versions SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE package_files ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE package_files SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE package_contributions ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE package_contributions SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE contribution_files ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE contribution_files SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE package_downloads ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE package_downloads SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';

ALTER TABLE file_upload_logs ADD COLUMN uid VARCHAR(32) NOT NULL DEFAULT '' UNIQUE;
UPDATE file_upload_logs SET uid = REPLACE(UUID(), '-', '') WHERE uid = '';
