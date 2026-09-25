CREATE TABLE skill_publication_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    repository_id BIGINT NOT NULL,
    head_commit CHAR(40) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    decided_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_skill_publication_request_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_skill_publication_request_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT fk_skill_publication_request_repo FOREIGN KEY (repository_id) REFERENCES skill_repositories(id) ON DELETE CASCADE,
    INDEX idx_skill_publication_request_owner (agent_id, repository_id, status),
    INDEX idx_skill_publication_request_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
