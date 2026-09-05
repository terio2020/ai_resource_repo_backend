-- Agent-authored user profile memories. The platform stores and shares the
-- structured profile supplied by the Agent; it never derives a profile from
-- GENERAL memories.
ALTER TABLE memories
    MODIFY COLUMN agent_id BIGINT NULL,
    ADD COLUMN memory_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN sharing_scope VARCHAR(30) NOT NULL DEFAULT 'AGENT_PRIVATE',
    ADD COLUMN owner_type VARCHAR(20) NOT NULL DEFAULT 'AGENT',
    ADD COLUMN client_memory_key VARCHAR(200) NULL,
    ADD COLUMN schema_version VARCHAR(20) NULL,
    ADD COLUMN revision INT NOT NULL DEFAULT 1,
    ADD INDEX idx_memory_user_type (user_id, memory_type),
    ADD UNIQUE INDEX uk_memory_agent_client_key (user_id, agent_id, client_memory_key);

-- The baseline FK cascaded every Memory when its Agent was deleted. Profile
-- Memory is user-owned, so change that relationship to SET NULL as a database-
-- level safeguard (the service also detaches profile rows before Agent delete).
SET @memory_agent_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'memories'
      AND COLUMN_NAME = 'agent_id'
      AND REFERENCED_TABLE_NAME = 'agents'
    LIMIT 1
);
SET @drop_memory_agent_fk = IF(
    @memory_agent_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE memories DROP FOREIGN KEY `', REPLACE(@memory_agent_fk, '`', '``'), '`')
);
PREPARE drop_memory_agent_fk_stmt FROM @drop_memory_agent_fk;
EXECUTE drop_memory_agent_fk_stmt;
DEALLOCATE PREPARE drop_memory_agent_fk_stmt;

ALTER TABLE memories
    ADD CONSTRAINT fk_memories_agent
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE SET NULL;

CREATE TABLE profile_memory_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(32) NOT NULL UNIQUE,
    memory_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source_agent_id BIGINT NULL,
    item_key VARCHAR(200) NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    fact_key VARCHAR(200) NOT NULL,
    value_type VARCHAR(30) NOT NULL,
    value_json JSON NOT NULL,
    value_hash VARCHAR(64) NOT NULL,
    context_json JSON NULL,
    context_hash VARCHAR(64) NOT NULL,
    record_type VARCHAR(30) NOT NULL,
    confidence DECIMAL(5,4) NULL,
    sensitivity VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    observed_at DATETIME NULL,
    valid_until DATETIME NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_item_memory FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_item_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_item_agent FOREIGN KEY (source_agent_id) REFERENCES agents(id) ON DELETE SET NULL,
    UNIQUE KEY uk_profile_memory_item (memory_id, item_key),
    INDEX idx_profile_user_identity (user_id, namespace, fact_key, context_hash),
    INDEX idx_profile_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
