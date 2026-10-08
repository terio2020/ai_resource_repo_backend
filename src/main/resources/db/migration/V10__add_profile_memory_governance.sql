-- User governance for Agent-authored profile memories.
-- Existing Agents receive an explicit wildcard grant to preserve the V8
-- USER_AGENTS behavior; users can subsequently replace or revoke it.
CREATE TABLE profile_memory_grants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_grant_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_grant_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    UNIQUE KEY uk_profile_grant_scope (user_id, agent_id, namespace),
    INDEX idx_profile_grant_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO profile_memory_grants (uid, user_id, agent_id, namespace)
SELECT LOWER(REPLACE(UUID(), '-', '')), user_id, id, '*'
FROM agents;

CREATE TABLE profile_memory_item_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(32) NOT NULL UNIQUE,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    previous_value_json JSON NULL,
    new_value_json JSON NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_history_item FOREIGN KEY (item_id) REFERENCES profile_memory_items(id) ON DELETE CASCADE,
    INDEX idx_profile_history_item (item_id, created_at),
    INDEX idx_profile_history_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
