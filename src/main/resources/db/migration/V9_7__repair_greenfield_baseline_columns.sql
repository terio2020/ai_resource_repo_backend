-- V1 creates several tables with fewer columns than the live schema and mappers.
-- Existing databases already have these columns; a fresh Flyway database does not.
-- Add only missing columns so this migration is safe for both paths.

SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_challenges' AND COLUMN_NAME = 'answer'), 'ALTER TABLE verification_challenges ADD COLUMN answer DECIMAL(20,6) NULL', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_challenges' AND COLUMN_NAME = 'attempt_count'), 'ALTER TABLE verification_challenges ADD COLUMN attempt_count INT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_challenges' AND COLUMN_NAME = 'max_attempts'), 'ALTER TABLE verification_challenges ADD COLUMN max_attempts INT NULL DEFAULT 5', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_challenges' AND COLUMN_NAME = 'expires_at'), 'ALTER TABLE verification_challenges ADD COLUMN expires_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'verification_challenges' AND COLUMN_NAME = 'consecutive_failures'), 'ALTER TABLE verification_challenges ADD COLUMN consecutive_failures INT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_upload_logs' AND COLUMN_NAME = 'file_type'), 'ALTER TABLE file_upload_logs ADD COLUMN file_type VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_upload_logs' AND COLUMN_NAME = 'upload_time'), 'ALTER TABLE file_upload_logs ADD COLUMN upload_time DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'memories' AND COLUMN_NAME = 'is_public'), 'ALTER TABLE memories ADD COLUMN is_public TINYINT(1) NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF(NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill_repositories' AND COLUMN_NAME = 'share_id'), 'ALTER TABLE skill_repositories ADD COLUMN share_id VARCHAR(22) NULL', 'SELECT 1');
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
