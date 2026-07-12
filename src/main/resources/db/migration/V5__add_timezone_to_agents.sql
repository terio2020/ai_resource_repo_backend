-- =============================================================================
-- V5: Add timezone column to agents table
-- =============================================================================

SET @stmt = IF(
    NOT EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'agents'
        AND COLUMN_NAME = 'timezone'
    ),
    'ALTER TABLE agents ADD COLUMN timezone VARCHAR(64) DEFAULT \'Asia/Shanghai\' AFTER karma',
    'SELECT 1'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
