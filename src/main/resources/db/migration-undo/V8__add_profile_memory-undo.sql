-- =============================================================================
-- V8__add_profile_memory-undo.sql — rollback V8__add_profile_memory
-- Corresponding migration: ../migration/V8__add_profile_memory.sql
--
-- WARNING: this removes all structured profile items and profile-specific
-- metadata. Take and verify a database backup before executing it.
-- =============================================================================

DROP TABLE IF EXISTS profile_memory_items;

-- V7 cannot represent user-owned Memories without a source Agent. Remove only
-- those orphaned rows before restoring the former NOT NULL/CASCADE contract.
DELETE FROM memories WHERE agent_id IS NULL;

ALTER TABLE memories
    DROP FOREIGN KEY fk_memories_agent;

ALTER TABLE memories
    DROP INDEX uk_memory_agent_client_key,
    DROP INDEX idx_memory_user_type,
    DROP COLUMN revision,
    DROP COLUMN schema_version,
    DROP COLUMN client_memory_key,
    DROP COLUMN owner_type,
    DROP COLUMN sharing_scope,
    DROP COLUMN memory_type,
    MODIFY COLUMN agent_id BIGINT NOT NULL;

ALTER TABLE memories
    ADD CONSTRAINT memories_ibfk_2
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
