-- Candidate schema, intentionally outside Flyway until migration order is registered.
-- UTC DATETIME values are supplied by the application. No production bootstrap.
CREATE TABLE playground_participations (
 agent_id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL,
 version BIGINT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT FALSE,
 max_decisions INT NOT NULL, max_attempts INT NOT NULL, max_daily_attempts INT NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY (agent_id) REFERENCES agents(id), FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE playground_activities (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, host_agent_id BIGINT NOT NULL, guest_agent_id BIGINT NOT NULL,
 horizon_months INT NOT NULL, status VARCHAR(20) NOT NULL, state_json LONGTEXT NOT NULL,
 next_sequence BIGINT NOT NULL DEFAULT 1, expires_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY (host_agent_id) REFERENCES agents(id), FOREIGN KEY (guest_agent_id) REFERENCES agents(id),
 INDEX idx_playground_host (host_agent_id, id), INDEX idx_playground_guest (guest_agent_id, id),
 INDEX idx_playground_expiry (status, expires_at)
) ENGINE=InnoDB;
CREATE TABLE playground_seats (
 agent_id BIGINT PRIMARY KEY, activity_id BIGINT NOT NULL, permission_version BIGINT NOT NULL,
 decisions_used INT NOT NULL DEFAULT 0, attempts_used INT NOT NULL DEFAULT 0,
 FOREIGN KEY (agent_id) REFERENCES agents(id), FOREIGN KEY (activity_id) REFERENCES playground_activities(id),
 INDEX idx_playground_seat_activity (activity_id)
) ENGINE=InnoDB;
CREATE TABLE playground_tasks (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, activity_id BIGINT NOT NULL, agent_id BIGINT NOT NULL,
 permission_version BIGINT NOT NULL, status VARCHAR(16) NOT NULL, phase VARCHAR(20) NOT NULL,
 expires_at DATETIME(6) NOT NULL, lease_hash CHAR(64), lease_expires_at DATETIME(6), attempt_id BIGINT,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY (activity_id) REFERENCES playground_activities(id), FOREIGN KEY (agent_id) REFERENCES agents(id),
 INDEX idx_playground_task_queue (agent_id, status, id), INDEX idx_playground_task_expiry (status, expires_at)
) ENGINE=InnoDB;
CREATE TABLE playground_attempts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, task_id BIGINT NOT NULL, agent_id BIGINT NOT NULL,
 request_key CHAR(36) NOT NULL, lease_hash CHAR(64) NOT NULL, status VARCHAR(16) NOT NULL,
 failure_reason VARCHAR(32),
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY (task_id) REFERENCES playground_tasks(id),
 UNIQUE KEY uq_playground_attempt (agent_id, request_key)
) ENGINE=InnoDB;
CREATE TABLE playground_daily_budgets (
 agent_id BIGINT NOT NULL, budget_date DATE NOT NULL, attempts_used INT NOT NULL DEFAULT 0, games_started INT NOT NULL DEFAULT 0,
 PRIMARY KEY (agent_id, budget_date), FOREIGN KEY (agent_id) REFERENCES agents(id)
) ENGINE=InnoDB;
CREATE TABLE playground_actions (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, agent_id BIGINT NOT NULL, task_id BIGINT NOT NULL,
 request_key CHAR(36) NOT NULL, request_hash CHAR(64) NOT NULL, response_json LONGTEXT NOT NULL,
 created_at DATETIME(6) NOT NULL, UNIQUE KEY uq_playground_action (agent_id, request_key),
 FOREIGN KEY (task_id) REFERENCES playground_tasks(id)
) ENGINE=InnoDB;
CREATE TABLE playground_events (
 activity_id BIGINT NOT NULL, sequence BIGINT NOT NULL, event_json LONGTEXT NOT NULL,
 PRIMARY KEY (activity_id, sequence), FOREIGN KEY (activity_id) REFERENCES playground_activities(id)
) ENGINE=InnoDB;

-- A terminal game's anonymous result link is created when its summary is first read.
CREATE TABLE playground_shares (
 activity_id BIGINT PRIMARY KEY, payload_json LONGTEXT NOT NULL,
 public_token CHAR(43) NOT NULL UNIQUE, created_at DATETIME(6) NOT NULL,
 FOREIGN KEY (activity_id) REFERENCES playground_activities(id)
) ENGINE=InnoDB;

CREATE TABLE playground_match_mutex (id INT PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO playground_match_mutex(id) VALUES(1);
CREATE TABLE playground_match_queue (
 agent_id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, permission_version BIGINT NOT NULL,
 mode VARCHAR(8) NOT NULL, brief_json LONGTEXT NOT NULL, status VARCHAR(16) NOT NULL,
 activity_id BIGINT, retries INT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, expires_at DATETIME(6) NOT NULL,
 FOREIGN KEY(agent_id) REFERENCES agents(id), FOREIGN KEY(user_id) REFERENCES users(id),
 INDEX idx_match_wait(status, mode, expires_at)
) ENGINE=InnoDB;
