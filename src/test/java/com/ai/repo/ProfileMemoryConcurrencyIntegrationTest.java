package com.ai.repo;

import com.ai.repo.dto.ProfileMemoryItemRequest;
import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.ProfileMemoryItem;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.ProfileMemoryItemMapper;
import com.ai.repo.service.ProfileMemoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "PROFILE_MEMORY_CONCURRENCY_IT", matches = "true")
class ProfileMemoryConcurrencyIntegrationTest {

    @Autowired
    private ProfileMemoryService profileMemoryService;
    @Autowired
    private MemoryMapper memoryMapper;
    @Autowired
    private ProfileMemoryItemMapper itemMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long agentId;

    @BeforeEach
    void alignFreshSchemaWithProductionVisibilityColumn() {
        Integer present = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'memories' AND column_name = 'is_public'",
                Integer.class);
        if (present != null && present == 0) {
            jdbcTemplate.execute("ALTER TABLE memories ADD COLUMN is_public BOOLEAN DEFAULT FALSE");
        }
    }

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM profile_memory_items WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM memories WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM agents WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void concurrentPatchesMustKeepHighestRevisionAndItsValue() throws Exception {
        createPrincipalRows();
        profileMemoryService.upsert(memory(), payload(1));

        int highestRevision = 10;
        CountDownLatch ready = new CountDownLatch(highestRevision - 1);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(highestRevision - 1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int revision = 2; revision <= highestRevision; revision++) {
                int candidate = revision;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        profileMemoryService.upsert(memory(), payload(candidate));
                    } catch (com.ai.repo.exception.BusinessException e) {
                        assertEquals(409, e.getCode());
                    }
                    return null;
                }));
            }
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        Memory stored = memoryMapper.selectByUserIdAndAgentIdAndClientKey(
                userId, agentId, "concurrency-profile");
        assertEquals(highestRevision, stored.getRevision());
        List<ProfileMemoryItem> items = itemMapper.selectByMemoryId(stored.getId());
        assertEquals(1, items.size());
        assertEquals("\"revision-10\"", items.get(0).getValueJson());
    }

    @Test
    void concurrentFirstCreateMustBeIdempotentAndCreateOneParent() throws Exception {
        createPrincipalRows();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    profileMemoryService.upsert(memory(), payload(1));
                    return null;
                }));
            }
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        Integer parentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = ? AND agent_id = ? AND client_memory_key = ?",
                Integer.class, userId, agentId, "concurrency-profile");
        assertEquals(1, parentCount);
        Memory stored = memoryMapper.selectByUserIdAndAgentIdAndClientKey(
                userId, agentId, "concurrency-profile");
        assertEquals(1, itemMapper.selectByMemoryId(stored.getId()).size());
    }

    private void createPrincipalRows() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "profile_it_" + suffix;
        jdbcTemplate.update(
                "INSERT INTO users (uid, username, password, email) VALUES (?, ?, ?, ?)",
                "u" + suffix, username, "test-only", username + "@example.invalid");
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?", Long.class, username);

        String code = "profile-it-" + suffix;
        jdbcTemplate.update(
                "INSERT INTO agents (uid, user_id, name, code) VALUES (?, ?, ?, ?)",
                "a" + suffix, userId, "Profile concurrency test", code);
        agentId = jdbcTemplate.queryForObject(
                "SELECT id FROM agents WHERE code = ?", Long.class, code);
    }

    private Memory memory() {
        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setAgentId(agentId);
        memory.setTitle("Concurrency profile");
        memory.setContent("Agent-authored test profile");
        memory.setMemoryType("USER_PROFILE");
        memory.setSharingScope("USER_AGENTS");
        memory.setOwnerType("USER");
        memory.setClientMemoryKey("concurrency-profile");
        memory.setIsPublic(false);
        memory.setStatus("VISIBLE");
        memory.setDownloadCount(0);
        memory.setLikeCount(0);
        return memory;
    }

    private ProfileMemoryPayload payload(int revision) {
        ProfileMemoryItemRequest item = new ProfileMemoryItemRequest();
        item.setItemKey("concurrent-value");
        item.setNamespace("test.concurrent");
        item.setKey("winner");
        item.setValueType("string");
        item.setValue("revision-" + revision);
        item.setContext(Map.of("scope", "integration-test"));
        item.setRecordType("FACT");

        ProfileMemoryPayload payload = new ProfileMemoryPayload();
        payload.setMode("PATCH");
        payload.setSchemaVersion("1.0");
        payload.setRevision(revision);
        payload.setItems(List.of(item));
        return payload;
    }
}
