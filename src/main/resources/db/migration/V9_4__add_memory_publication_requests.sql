ALTER TABLE skill_publication_requests ADD COLUMN metadata_hash CHAR(64) NULL;

CREATE TABLE memory_publication_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    memory_id BIGINT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    decided_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_memory_publication_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_memory_publication_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT fk_memory_publication_memory FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE,
    INDEX idx_memory_publication_owner (agent_id, status),
    INDEX idx_memory_publication_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
