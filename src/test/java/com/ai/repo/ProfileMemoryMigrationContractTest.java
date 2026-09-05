package com.ai.repo;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileMemoryMigrationContractTest {

    @Test
    void migrationPreservesUserOwnedProfileMemoryAndCreatesStructuredItems() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V8__add_profile_memory.sql"));

        assertTrue(sql.contains("MODIFY COLUMN agent_id BIGINT NULL"));
        assertTrue(sql.contains("FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE SET NULL"));
        assertTrue(sql.contains("CREATE TABLE profile_memory_items"));
        assertTrue(sql.contains("UNIQUE KEY uk_profile_memory_item (memory_id, item_key)"));
        assertTrue(sql.contains("INDEX idx_profile_user_identity (user_id, namespace, fact_key, context_hash)"));
        assertTrue(sql.contains("FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE"));
    }

    @Test
    void mapperUsesDatabaseSerializationAndAtomicRevisionAdvance() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/mapper/MemoryMapper.xml"));

        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)"));
        assertTrue(sql.contains("<select id=\"selectProfileByKeyForUpdate\""));
        assertTrue(sql.contains("FOR UPDATE"));
        assertTrue(sql.contains("<update id=\"updateProfileIfRevisionOlder\""));
        assertTrue(sql.contains("revision &lt; #{revision}"));
    }

    @Test
    void undoMigrationRemovesProfileSchemaAndRestoresAgentCascade() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration-undo/V8__add_profile_memory-undo.sql"));

        assertTrue(sql.contains("DROP TABLE IF EXISTS profile_memory_items"));
        assertTrue(sql.contains("DELETE FROM memories WHERE agent_id IS NULL"));
        assertTrue(sql.contains("DROP FOREIGN KEY fk_memories_agent"));
        assertTrue(sql.contains("DROP COLUMN memory_type"));
        assertTrue(sql.contains("MODIFY COLUMN agent_id BIGINT NOT NULL"));
        assertTrue(sql.contains("FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE"));
    }
}
