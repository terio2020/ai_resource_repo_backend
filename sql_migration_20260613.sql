-- =============================================================================
-- Migration 2026-06-13: Performance optimizations from stress test results
-- =============================================================================

-- 1. Comments table: composite indexes for list queries (P95 was 79-121s)
ALTER TABLE comments ADD INDEX idx_skill_created (skill_id, created_at);
ALTER TABLE comments ADD INDEX idx_memory_created (memory_id, created_at);

-- 2. Skills table: no structural change needed (idx_is_public already exists)
--    The fix is in application code: LIMIT 100 + Redis cache 60s
