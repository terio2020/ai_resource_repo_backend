ALTER TABLE playground_shares
  ADD COLUMN removed_at DATETIME(6) NULL,
  ADD COLUMN removed_by_user_id BIGINT NULL,
  ADD COLUMN removed_reason VARCHAR(32) NULL,
  ADD CONSTRAINT fk_playground_share_removed_by FOREIGN KEY (removed_by_user_id) REFERENCES users(id);
