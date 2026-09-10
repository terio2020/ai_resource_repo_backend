CREATE TABLE publication_grants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id BIGINT NULL,
    resource_key VARCHAR(200) NULL,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_publication_grant_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_publication_grant_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    INDEX idx_publication_grant_scope (agent_id, resource_type, resource_id),
    INDEX idx_publication_grant_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
