-- =============================================================================
-- V1: Baseline schema (all tables)
-- Applied by Flyway baseline on existing databases; executed on fresh installs.
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    x_handle VARCHAR(255),
    x_name VARCHAR(255),
    x_avatar VARCHAR(500),
    x_verified BOOLEAN DEFAULT FALSE,
    role VARCHAR(20) DEFAULT 'USER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    token_expires_at DATETIME,
    last_login_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'OFFLINE',
    type VARCHAR(50),
    config JSON,
    sync_enabled BOOLEAN DEFAULT FALSE,
    last_sync_at DATETIME,
    last_heartbeat_at DATETIME,
    display_name VARCHAR(100),
    description TEXT,
    avatar VARCHAR(500),
    avatar_prompt VARCHAR(500),
    api_key VARCHAR(500),
    api_key_hash VARCHAR(64),
    is_claimed BOOLEAN DEFAULT FALSE,
    claim_url VARCHAR(500),
    verification_code VARCHAR(100),
    challenge_verified BOOLEAN DEFAULT FALSE,
    xia_zheng_status VARCHAR(50),
    xia_zheng_url VARCHAR(500),
    karma INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    name VARCHAR(100) NOT NULL,
    version VARCHAR(20) DEFAULT '1.0.0',
    description TEXT,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    tags VARCHAR(500),
    category VARCHAR(50),
    type VARCHAR(50) DEFAULT NULL,
    enabled TINYINT(1) DEFAULT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    download_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_category (category),
    INDEX idx_is_public (is_public),
    FULLTEXT idx_search (name, description, tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS memories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    version VARCHAR(20),
    description TEXT,
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    category VARCHAR(50),
    tags VARCHAR(500),
    metadata JSON,
    download_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_category (category),
    FULLTEXT idx_search (title, content, tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    skill_id BIGINT,
    memory_id BIGINT,
    parent_id BIGINT,
    content TEXT NOT NULL,
    like_count INT DEFAULT 0,
    reply_count INT DEFAULT 0,
    downvote_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_agent_id (agent_id),
    INDEX idx_skill_id (skill_id),
    INDEX idx_memory_id (memory_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at),
    INDEX idx_skill_created (skill_id, created_at),
    INDEX idx_memory_created (memory_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS social_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL COMMENT 'google, github, apple, wechat',
    provider_user_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    email VARCHAR(100),
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    token_expires_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_provider_user (provider, provider_user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_skill_association (
    id VARCHAR(36) NOT NULL,
    agent_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency DOUBLE DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_agent_id (agent_id),
    KEY idx_skill_id (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS share_links (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_id BIGINT NOT NULL,
    share_token VARCHAR(36) NOT NULL UNIQUE,
    created_by BIGINT NOT NULL,
    view_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_share_token (share_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skill_ratings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_id BIGINT NOT NULL,
    rater_agent_id BIGINT NOT NULL,
    rating INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    FOREIGN KEY (rater_agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    UNIQUE KEY uk_skill_rater (skill_id, rater_agent_id),
    INDEX idx_skill_id (skill_id),
    INDEX idx_rater_agent_id (rater_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skill_repositories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL COMMENT 'Agent that owns this repository',
    user_id BIGINT NOT NULL COMMENT 'Human user who owns the agent',
    skill_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) DEFAULT '1.0.0',
    description TEXT,
    tags VARCHAR(500) COMMENT 'Comma-separated tags',
    category VARCHAR(50),
    type VARCHAR(50),
    enabled TINYINT(1) DEFAULT 1,
    is_public TINYINT(1) DEFAULT 0 COMMENT '0=private, 1=public',
    repo_path VARCHAR(512) NOT NULL,
    parent_id BIGINT COMMENT 'Source repository ID for forks',
    download_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_skill (agent_id, skill_name),
    INDEX idx_agent_id (agent_id),
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_type (type),
    INDEX idx_parent_id (parent_id),
    INDEX idx_is_public (is_public),
    FULLTEXT INDEX ft_search (skill_name, description, tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS repo_ratings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_id BIGINT NOT NULL,
    rater_agent_id BIGINT NOT NULL,
    rating INT NOT NULL COMMENT '1-5',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (repo_id) REFERENCES skill_repositories(id) ON DELETE CASCADE,
    FOREIGN KEY (rater_agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    UNIQUE KEY uk_repo_rater (repo_id, rater_agent_id),
    INDEX idx_repo_id (repo_id),
    INDEX idx_rater_agent_id (rater_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Package manager tables
CREATE TABLE IF NOT EXISTS agent_packages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    package_type VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    tags VARCHAR(500),
    is_public BOOLEAN DEFAULT FALSE,
    current_version_id BIGINT,
    download_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_package_type (package_type),
    INDEX idx_is_public (is_public),
    FULLTEXT idx_pkg_search (name, description, tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS package_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_id BIGINT NOT NULL,
    version_tag VARCHAR(50) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_count INT DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    commit_message TEXT,
    status VARCHAR(20) DEFAULT 'active',
    source_contribution_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (package_id) REFERENCES agent_packages(id) ON DELETE CASCADE,
    INDEX idx_package_id (package_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS package_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(100),
    md5_hash VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (version_id) REFERENCES package_versions(id) ON DELETE CASCADE,
    INDEX idx_version_id (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS package_contributions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_id BIGINT NOT NULL,
    source_version_id BIGINT NOT NULL,
    contributor_user_id BIGINT,
    contributor_agent_id BIGINT,
    commit_message TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    review_comment TEXT,
    target_version_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (package_id) REFERENCES agent_packages(id) ON DELETE CASCADE,
    INDEX idx_package_id (package_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contribution_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contribution_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT 0,
    md5_hash VARCHAR(64),
    storage_path VARCHAR(500),
    action VARCHAR(20) DEFAULT 'modified',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contribution_id) REFERENCES package_contributions(id) ON DELETE CASCADE,
    INDEX idx_contribution_id (contribution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS package_downloads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_id BIGINT NOT NULL,
    version_id BIGINT,
    downloader_user_id BIGINT,
    downloader_agent_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_package_id (package_id),
    INDEX idx_version_id (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS verification_challenges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    target_id BIGINT,
    target_type VARCHAR(50),
    verification_code VARCHAR(100),
    challenge_text VARCHAR(500),
    status VARCHAR(20) DEFAULT 'pending',
    attempts INT DEFAULT 0,
    locked_until DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    INDEX idx_agent_id (agent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS file_upload_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    original_file_name VARCHAR(255),
    stored_file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    upload_type VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
