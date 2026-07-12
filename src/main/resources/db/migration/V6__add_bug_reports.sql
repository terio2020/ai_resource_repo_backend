-- =============================================================================
-- V6: Add bug_reports table for agent bug reporting
-- =============================================================================

CREATE TABLE IF NOT EXISTS bug_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(32) NOT NULL UNIQUE,
    agent_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(20) NOT NULL DEFAULT 'info',
    source VARCHAR(500),
    environment TEXT,
    steps_to_reproduce TEXT,
    expected_behavior TEXT,
    actual_behavior TEXT,
    stack_trace MEDIUMTEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    category VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bug_reports_agent_id (agent_id),
    INDEX idx_bug_reports_severity (severity),
    INDEX idx_bug_reports_status (status),
    CONSTRAINT fk_bug_reports_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;