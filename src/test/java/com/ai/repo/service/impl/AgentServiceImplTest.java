package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AgentIdCount;
import com.ai.repo.dto.AgentResourceCounts;
import com.ai.repo.dto.AgentStatsResponse;
import com.ai.repo.dto.AgentSyncResponse;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.CommentMapper;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.NotificationMapper;
import com.ai.repo.util.ApiKeyHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

    @Mock
    private AgentMapper agentMapper;

    @Mock
    private MemoryMapper memoryMapper;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ApiKeyHashUtil apiKeyHashUtil;

    private AgentServiceImpl agentService;

    private static final Path TEST_BASE_PATH = Paths.get("/tmp/test-agent-avatars");

    @BeforeEach
    void setUp() throws Exception {
        agentService = new AgentServiceImpl();
        try {
            java.lang.reflect.Field agentField = AgentServiceImpl.class.getDeclaredField("agentMapper");
            agentField.setAccessible(true);
            agentField.set(agentService, agentMapper);

            java.lang.reflect.Field memoryField = AgentServiceImpl.class.getDeclaredField("memoryMapper");
            memoryField.setAccessible(true);
            memoryField.set(agentService, memoryMapper);

            java.lang.reflect.Field notifField = AgentServiceImpl.class.getDeclaredField("notificationMapper");
            notifField.setAccessible(true);
            notifField.set(agentService, notificationMapper);

            java.lang.reflect.Field commentField = AgentServiceImpl.class.getDeclaredField("commentMapper");
            commentField.setAccessible(true);
            commentField.set(agentService, commentMapper);

            java.lang.reflect.Field hashField = AgentServiceImpl.class.getDeclaredField("apiKeyHashUtil");
            hashField.setAccessible(true);
            hashField.set(agentService, apiKeyHashUtil);

            java.lang.reflect.Field basePathField = AgentServiceImpl.class.getDeclaredField("basePath");
            basePathField.setAccessible(true);
            basePathField.set(agentService, TEST_BASE_PATH.toString());
        } catch (Exception e) {
            fail("Failed to inject fields: " + e.getMessage());
        }

        if (Files.exists(TEST_BASE_PATH)) {
            Files.walk(TEST_BASE_PATH)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }

        // Pass-through hash so existing tests that set/get apiKey still match
        lenient().when(apiKeyHashUtil.hash(anyString()))
                .thenAnswer(inv -> "hashed_" + inv.<String>getArgument(0));
    }

    // ========== create() Tests ==========

    @Test
    void create_shouldSucceed_whenCodeIsUnique() {
        Agent agent = new Agent();
        agent.setUserId(1L);
        agent.setName("Test Agent");
        agent.setCode("test-agent-001");
        agent.setStatus("ACTIVE");
        agent.setApiKey("raw-key-001");

        when(agentMapper.selectByCode("test-agent-001")).thenReturn(null);
        when(agentMapper.insert(any(Agent.class))).thenReturn(1);

        Agent result = agentService.create(agent);

        assertNotNull(result);
        assertEquals("test-agent-001", result.getCode());
        assertEquals("hashed_raw-key-001", result.getApiKeyHash());
        assertNotNull(result.getAvatar());
        assertTrue(result.getAvatar().contains("/avatars/agents/"));
        verify(agentMapper).insert(any(Agent.class));
    }

    @Test
    void create_shouldKeepExistingAvatar_whenProvided() {
        Agent agent = new Agent();
        agent.setUserId(1L);
        agent.setName("Test Agent");
        agent.setCode("test-agent-002");
        agent.setAvatar("/custom/avatar.png");

        when(agentMapper.selectByCode("test-agent-002")).thenReturn(null);
        when(agentMapper.insert(any(Agent.class))).thenReturn(1);

        Agent result = agentService.create(agent);

        assertNotNull(result);
        assertEquals("/custom/avatar.png", result.getAvatar());
        verify(agentMapper).insert(any(Agent.class));
    }

    @Test
    void create_shouldThrowException_whenCodeAlreadyExists() {
        // Given
        Agent agent = new Agent();
        agent.setUserId(1L);
        agent.setName("Test Agent");
        agent.setCode("duplicate-code");

        Agent existingAgent = new Agent();
        existingAgent.setId(1L);
        existingAgent.setCode("duplicate-code");

        when(agentMapper.selectByCode("duplicate-code")).thenReturn(existingAgent);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.create(agent);
        });
        assertTrue(exception.getMessage().contains("already exists"));
        verify(agentMapper, never()).insert(any());
    }

    // ========== update() Tests ==========

    @Test
    void update_shouldSucceed_whenAgentExists() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setName("Updated Agent");

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(agentMapper.update(any(Agent.class))).thenReturn(1);

        // When
        Agent result = agentService.update(agent);

        // Then
        assertNotNull(result);
        verify(agentMapper).update(any(Agent.class));
    }

    @Test
    void update_shouldThrowException_whenAgentNotFound() {
        // Given
        Agent agent = new Agent();
        agent.setId(999L);
        agent.setName("Non-existent Agent");

        when(agentMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.update(agent);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    // ========== delete() Tests ==========

    @Test
    void delete_shouldSucceed_whenAgentExists() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(memoryMapper.deleteByAgentId(1L)).thenReturn(1);
        when(notificationMapper.deleteByAgentId(1L)).thenReturn(1);
        when(commentMapper.deleteByAgentId(1L)).thenReturn(1);
        when(agentMapper.deleteById(1L)).thenReturn(1);

        // When
        boolean result = agentService.delete(1L);

        // Then
        assertTrue(result);
        verify(memoryMapper).deleteByAgentId(1L);
        verify(notificationMapper).deleteByAgentId(1L);
        verify(commentMapper).deleteByAgentId(1L);
        verify(agentMapper).deleteById(1L);
    }

    @Test
    void delete_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.delete(999L);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    // ========== findById() Tests ==========

    @Test
    void findById_shouldReturnAgent_whenFound() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setName("Test Agent");

        when(agentMapper.selectById(1L)).thenReturn(agent);

        // When
        Agent result = agentService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldReturnNull_whenNotFound() {
        // Given
        when(agentMapper.selectById(999L)).thenReturn(null);

        // When
        Agent result = agentService.findById(999L);

        // Then
        assertNull(result);
    }

    // ========== findByCode() Tests ==========

    @Test
    void findByCode_shouldReturnAgent_whenFound() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setCode("test-code");

        when(agentMapper.selectByCode("test-code")).thenReturn(agent);

        // When
        Agent result = agentService.findByCode("test-code");

        // Then
        assertNotNull(result);
        assertEquals("test-code", result.getCode());
    }

    @Test
    void findByCode_shouldReturnNull_whenNotFound() {
        // Given
        when(agentMapper.selectByCode("nonexistent")).thenReturn(null);

        // When
        Agent result = agentService.findByCode("nonexistent");

        // Then
        assertNull(result);
    }

    // ========== findAll() Tests ==========

    @Test
    void findAll_shouldReturnAllAgents() {
        // Given
        Agent agent1 = new Agent();
        agent1.setId(1L);
        Agent agent2 = new Agent();
        agent2.setId(2L);

        when(agentMapper.selectAll()).thenReturn(Arrays.asList(agent1, agent2));

        // When
        var result = agentService.findAll();

        // Then
        assertEquals(2, result.size());
    }

    // ========== findByUserId() Tests ==========

    @Test
    void findByUserId_shouldReturnAgentsForUser() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setUserId(1L);

        when(agentMapper.selectByUserId(1L)).thenReturn(Arrays.asList(agent));

        // When
        var result = agentService.findByUserId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    // ========== findPage() Tests ==========

    @Test
    void findPage_shouldReturnPaginatedResults() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);

        when(agentMapper.selectPage(1, 10, 0)).thenReturn(Arrays.asList(agent));
        when(agentMapper.countTotal()).thenReturn(20L);

        // When
        PageResult<Agent> result = agentService.findPage(1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(20L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
    }

    @Test
    void findPage_shouldUseDefaults_whenPageIsNull() {
        // Given
        when(agentMapper.selectPage(1, 10, 0)).thenReturn(Collections.emptyList());
        when(agentMapper.countTotal()).thenReturn(0L);

        // When
        PageResult<Agent> result = agentService.findPage(null, null);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
    }

    // ========== getStats() Tests ==========

    @Test
    void getStats_shouldReturnStats_whenAgentFound() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);

        AgentStatsResponse stats = new AgentStatsResponse();
        stats.setMemoryCount(10L);

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(agentMapper.selectStats(1L)).thenReturn(stats);

        // When
        AgentStatsResponse result = agentService.getStats(1L);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getMemoryCount());
    }

    @Test
    void getStats_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.getStats(999L);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    // ========== updateHeartbeat() Tests ==========

    @Test
    void updateHeartbeat_shouldSucceed_whenValidStatus() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        String timestamp = LocalDateTime.now().toString();

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(agentMapper.updateHeartbeat(eq(1L), eq("ACTIVE"), anyString())).thenReturn(1);

        // When
        boolean result = agentService.updateHeartbeat(1L, "ACTIVE", timestamp);

        // Then
        assertTrue(result);
    }

    @Test
    void updateHeartbeat_shouldThrowException_whenInvalidStatus() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        String timestamp = LocalDateTime.now().toString();

        when(agentMapper.selectById(1L)).thenReturn(agent);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.updateHeartbeat(1L, "INVALID_STATUS", timestamp);
        });
        assertTrue(exception.getMessage().contains("Invalid status"));
    }

    @Test
    void updateHeartbeat_shouldThrowException_whenAgentNotFound() {
        // Given
        String timestamp = LocalDateTime.now().toString();

        when(agentMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.updateHeartbeat(999L, "ACTIVE", timestamp);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    // ========== syncData() Tests ==========

    @Test
    void syncData_shouldReturnAllData_whenNoSinceParameter() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);

        Memory memory = new Memory();
        memory.setId(1L);
        memory.setTitle("Test Memory");
        memory.setUpdatedAt(LocalDateTime.now());

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(memoryMapper.selectByAgentId(1L)).thenReturn(Arrays.asList(memory));

        // When
        AgentSyncResponse result = agentService.syncData(1L, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getMemories().size());
    }

    @Test
    void syncData_shouldFilterBySinceParameter() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusHours(1);

        Agent agent = new Agent();
        agent.setId(1L);

        Memory oldMemory = new Memory();
        oldMemory.setId(1L);
        oldMemory.setTitle("Old Memory");
        oldMemory.setUpdatedAt(now.minusDays(1));

        Memory newMemory = new Memory();
        newMemory.setId(2L);
        newMemory.setTitle("New Memory");
        newMemory.setUpdatedAt(now.plusHours(1));

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(memoryMapper.selectByAgentId(1L)).thenReturn(Arrays.asList(oldMemory, newMemory));

        // When
        AgentSyncResponse result = agentService.syncData(1L, since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getMemories().size());
        assertEquals("New Memory", result.getMemories().get(0).getTitle());
    }

    @Test
    void syncData_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.syncData(999L, null);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    // ========== updateClaimStatus() Tests ==========

    @Test
    void updateClaimStatus_shouldSucceed_whenAgentExists() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(agentMapper.updateClaimStatus(1L, true)).thenReturn(1);

        // When
        boolean result = agentService.updateClaimStatus(1L, true);

        // Then
        assertTrue(result);
    }

    // ========== updateKarma() Tests ==========

    @Test
    void updateKarma_shouldSucceed_whenAgentExists() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(agentMapper.updateKarma(1L, 10)).thenReturn(1);

        // When
        boolean result = agentService.updateKarma(1L, 10);

        // Then
        assertTrue(result);
    }

    // ========== Additional Method Tests ==========

    @Test
    void updateStatusOnly_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.updateStatusOnly(999L, "ACTIVE");
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void updateConfigOnly_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.updateConfigOnly(999L, "{}");
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void updateAvatar_shouldSucceed_whenAgentExists() {
        Agent agent = new Agent();
        agent.setId(1L);

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(agentMapper.updateAvatar(1L, "/avatars/agents/1/new.png")).thenReturn(1);

        boolean result = agentService.updateAvatar(1L, "/avatars/agents/1/new.png");

        assertTrue(result);
        verify(agentMapper).updateAvatar(1L, "/avatars/agents/1/new.png");
    }

    @Test
    void updateAvatar_shouldThrowException_whenAgentNotFound() {
        when(agentMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.updateAvatar(999L, "/avatars/agents/999/new.png");
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void findByApiKey_shouldReturnAgent_whenFound() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setApiKey("test-api-key");
        agent.setApiKeyHash("hashed_test-api-key");

        when(agentMapper.selectByApiKeyHash("hashed_test-api-key")).thenReturn(agent);

        // When
        Agent result = agentService.findByApiKey("test-api-key");

        // Then
        assertNotNull(result);
        assertEquals("test-api-key", result.getApiKey());
        assertEquals("hashed_test-api-key", result.getApiKeyHash());
    }

    // ========== getResourceCounts() Tests ==========

    @Test
    void getResourceCounts_shouldReturnMergedCounts_whenBothMappersReturnData() {
        // Given
        List<Long> agentIds = Arrays.asList(1L, 2L, 3L);

        AgentIdCount mc2 = new AgentIdCount();
        mc2.setAgentId(2L);
        mc2.setCount(7);
        AgentIdCount mc3 = new AgentIdCount();
        mc3.setAgentId(3L);
        mc3.setCount(2);

        when(memoryMapper.selectCountByAgentIds(agentIds)).thenReturn(Arrays.asList(mc2, mc3));

        // When
        Map<Long, AgentResourceCounts> result = agentService.getResourceCounts(agentIds);

        // Then
        assertEquals(3, result.size());
        assertEquals(0, result.get(1L).getMemoryCount().intValue());
        assertEquals(7, result.get(2L).getMemoryCount().intValue());
        assertEquals(2, result.get(3L).getMemoryCount().intValue());
    }

    @Test
    void getResourceCounts_shouldReturnEmptyMap_whenEmptyInput() {
        // When
        Map<Long, AgentResourceCounts> result = agentService.getResourceCounts(Collections.emptyList());

        // Then
        assertTrue(result.isEmpty());
        verify(memoryMapper, never()).selectCountByAgentIds(any());
    }

    @Test
    void getResourceCounts_shouldReturnAllZeros_whenNoResults() {
        // Given
        List<Long> agentIds = Arrays.asList(1L, 2L);

        when(memoryMapper.selectCountByAgentIds(agentIds)).thenReturn(Collections.emptyList());

        // When
        Map<Long, AgentResourceCounts> result = agentService.getResourceCounts(agentIds);

        // Then
        assertEquals(2, result.size());
        assertEquals(0, result.get(1L).getMemoryCount().intValue());
        assertEquals(0, result.get(2L).getMemoryCount().intValue());
    }

    @Test
    void getResourceCounts_shouldReturnNullSafe_whenNullInput() {
        // When
        Map<Long, AgentResourceCounts> result = agentService.getResourceCounts(null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findByStatus_shouldReturnAgentsByStatus() {
        // Given
        Agent agent = new Agent();
        agent.setStatus("ACTIVE");

        when(agentMapper.selectByStatus("ACTIVE")).thenReturn(Arrays.asList(agent));

        // When
        var result = agentService.findByStatus("ACTIVE");

        // Then
        assertEquals(1, result.size());
    }

    @Test
    void findByType_shouldReturnAgentsByType() {
        // Given
        Agent agent = new Agent();
        agent.setType("assistant");

        when(agentMapper.selectByType("assistant")).thenReturn(Arrays.asList(agent));

        // When
        var result = agentService.findByType("assistant");

        // Then
        assertEquals(1, result.size());
    }
}