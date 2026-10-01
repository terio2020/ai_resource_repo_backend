-- Disable Playground and verify a database backup before reverting this migration.
-- Older application versions do not enforce takedown markers; do not re-enable
-- public shares on an older version after this undo, or removed links may reappear.
ALTER TABLE playground_shares DROP FOREIGN KEY fk_playground_share_removed_by;
ALTER TABLE playground_shares
  DROP COLUMN removed_reason,
  DROP COLUMN removed_by_user_id,
  DROP COLUMN removed_at;
