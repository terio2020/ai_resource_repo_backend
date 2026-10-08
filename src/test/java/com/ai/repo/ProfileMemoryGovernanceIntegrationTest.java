package com.ai.repo;

import com.ai.repo.dto.ProfileMemoryGovernRequest;
import com.ai.repo.dto.ProfileMemoryItemRequest;
import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.dto.ProfileMemoryResponse;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.ProfileMemoryItem;
import com.ai.repo.service.ProfileMemoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "PROFILE_MEMORY_GOVERNANCE_IT", matches = "true")
class ProfileMemoryGovernanceIntegrationTest {
    @Autowired
    private ProfileMemoryService profileMemoryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long sourceAgentId;
    private Long granteeAgentId;

    @BeforeEach
    void createFixture() {
        Integer present = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'memories' AND column_name = 'is_public'",
                Integer.class);
        if (present != null && present == 0) {
            jdbcTemplate.execute("ALTER TABLE memories ADD COLUMN is_public BOOLEAN DEFAULT FALSE");
        }

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "profile_gov_" + suffix;
        jdbcTemplate.update("INSERT INTO users (uid, username, password, email) VALUES (?, ?, ?, ?)",
                "u" + suffix, username, "test-only", username + "@example.invalid");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        sourceAgentId = insertAgent("source-" + suffix, "Source Agent");
        granteeAgentId = insertAgent("grantee-" + suffix, "Grantee Agent");

        Memory saved = profileMemoryService.upsert(memory(), payload());
        assertTrue(saved.getId() > 0);
    }

    @AfterEach
    void cleanUp() {
        if (userId == null) return;
        jdbcTemplate.update("DELETE FROM profile_memory_grants WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM profile_memory_items WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM memories WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM agents WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void grantsAndHumanGovernanceMustControlVisibleProfile() {
        ProfileMemoryResponse beforeGrant = profileMemoryService.findByUserIdVisibleToAgent(userId, granteeAgentId);
        assertTrue(beforeGrant.getItems().isEmpty());

        profileMemoryService.replaceGrants(userId, granteeAgentId, List.of("communication"));
        ProfileMemoryResponse afterGrant = profileMemoryService.findByUserIdVisibleToAgent(userId, granteeAgentId);
        assertEquals(1, afterGrant.getItems().size());
        assertEquals(1, afterGrant.getMemories().size());
        assertEquals(1, afterGrant.getMemories().get(0).getRevision());
        assertTrue(afterGrant.getMemories().get(0).getContent() == null);
        assertTrue(afterGrant.getMemories().get(0).getDescription() == null);

        ProfileMemoryItem item = afterGrant.getItems().get(0);
        ProfileMemoryGovernRequest request = new ProfileMemoryGovernRequest();
        request.setAction("CONFIRM");
        ProfileMemoryItem confirmed = profileMemoryService.governItem(userId, item.getId(), request);
        assertEquals("CONFIRMED", confirmed.getStatus());
        assertEquals(1, profileMemoryService.findItemHistory(userId, item.getId()).size());

        profileMemoryService.replaceGrants(userId, granteeAgentId, List.of());
        assertTrue(profileMemoryService.findByUserIdVisibleToAgent(userId, granteeAgentId).getItems().isEmpty());
    }

    private Long insertAgent(String code, String name) {
        jdbcTemplate.update("INSERT INTO agents (uid, user_id, name, code) VALUES (?, ?, ?, ?)",
                "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), userId, name, code);
        return jdbcTemplate.queryForObject("SELECT id FROM agents WHERE code = ?", Long.class, code);
    }

    private Memory memory() {
        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setAgentId(sourceAgentId);
        memory.setTitle("Governed profile");
        memory.setContent("Agent-authored profile");
        memory.setMemoryType("USER_PROFILE");
        memory.setSharingScope("USER_AGENTS");
        memory.setOwnerType("USER");
        memory.setClientMemoryKey("governed-profile");
        memory.setIsPublic(false);
        memory.setStatus("VISIBLE");
        memory.setDownloadCount(0);
        memory.setLikeCount(0);
        return memory;
    }

    private ProfileMemoryPayload payload() {
        ProfileMemoryItemRequest item = new ProfileMemoryItemRequest();
        item.setItemKey("primary-language");
        item.setNamespace("communication");
        item.setKey("language.primary");
        item.setValueType("string");
        item.setValue("zh-CN");
        item.setContext(Map.of("scope", "GLOBAL"));
        item.setRecordType("PREFERENCE");
        ProfileMemoryPayload payload = new ProfileMemoryPayload();
        payload.setMode("PATCH");
        payload.setSchemaVersion("1.0");
        payload.setRevision(1);
        payload.setItems(List.of(item));
        return payload;
    }
}
