package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminAgentResponse;
import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgentServiceImplTest {

    @Mock
    private AgentMapper agentMapper;

    private AdminAgentServiceImpl adminAgentService;

    @BeforeEach
    void setUp() throws Exception {
        adminAgentService = new AdminAgentServiceImpl();
        Field field = AdminAgentServiceImpl.class.getDeclaredField("agentMapper");
        field.setAccessible(true);
        field.set(adminAgentService, agentMapper);
    }

    private Agent agent(long id, String status) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setUid("uid-" + id);
        agent.setName("agent" + id);
        agent.setCode("code-" + id);
        agent.setStatus(status);
        agent.setKarma(5);
        return agent;
    }

    @Test
    void listAgents_shouldMapAndPage() {
        Agent a = agent(1L, "ACTIVE");
        a.setUserId(10L);
        when(agentMapper.selectBySearch(any())).thenReturn(Collections.singletonList(a));
        when(agentMapper.adminCount(eq("key"), eq("ACTIVE"), eq("BOT"))).thenReturn(7L);

        PageResult<AdminAgentResponse> result = adminAgentService.listAgents(1, 10, "key", "ACTIVE", "BOT");

        ArgumentCaptor<AgentSearchRequest> captor = ArgumentCaptor.forClass(AgentSearchRequest.class);
        verify(agentMapper).selectBySearch(captor.capture());
        AgentSearchRequest sent = captor.getValue();
        assertEquals("key", sent.getName());
        assertEquals("ACTIVE", sent.getStatus());
        assertEquals("BOT", sent.getType());
        assertEquals(1, sent.getPage());
        assertEquals(10, sent.getSize());
        assertEquals(0, sent.getOffset());

        assertEquals(7L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        AdminAgentResponse item = result.getRecords().get(0);
        assertEquals(1L, item.getId());
        assertEquals(10L, item.getUserId());
        assertEquals("agent1", item.getName());
        assertEquals(5, item.getKarma());
        assertNull(item.getDescription());
    }

    @Test
    void listAgents_shouldCalculateOffsetForLaterPages() {
        when(agentMapper.selectBySearch(any())).thenReturn(Collections.emptyList());
        when(agentMapper.adminCount(isNull(), isNull(), isNull())).thenReturn(0L);

        adminAgentService.listAgents(3, 12, null, null, null);

        ArgumentCaptor<AgentSearchRequest> captor = ArgumentCaptor.forClass(AgentSearchRequest.class);
        verify(agentMapper).selectBySearch(captor.capture());
        assertEquals(24, captor.getValue().getOffset());
    }

    @Test
    void listAgents_shouldEscapeKeyword() {
        when(agentMapper.selectBySearch(any())).thenReturn(Collections.emptyList());
        when(agentMapper.adminCount(eq("a\\_b"), any(), any())).thenReturn(0L);

        adminAgentService.listAgents(1, 10, "a_b", null, null);

        ArgumentCaptor<AgentSearchRequest> captor = ArgumentCaptor.forClass(AgentSearchRequest.class);
        verify(agentMapper).selectBySearch(captor.capture());
        assertEquals("a\\_b", captor.getValue().getName());
        verify(agentMapper).adminCount(eq("a\\_b"), isNull(), isNull());
    }

    @Test
    void listAgents_shouldDefaultPaging() {
        when(agentMapper.selectBySearch(any())).thenReturn(Collections.emptyList());
        when(agentMapper.adminCount(isNull(), isNull(), isNull())).thenReturn(0L);

        adminAgentService.listAgents(null, null, null, null, null);

        ArgumentCaptor<AgentSearchRequest> captor = ArgumentCaptor.forClass(AgentSearchRequest.class);
        verify(agentMapper).selectBySearch(captor.capture());
        assertEquals(1, captor.getValue().getPage());
        assertEquals(10, captor.getValue().getSize());
    }

    @Test
    void updateStatus_shouldUpdate() {
        when(agentMapper.selectById(3L)).thenReturn(agent(3L, "ACTIVE"));

        adminAgentService.updateStatus(1L, 3L, "DISABLED");

        verify(agentMapper).updateStatusOnly(3L, "DISABLED");
    }

    @Test
    void updateStatus_shouldRejectMissingAgent() {
        when(agentMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminAgentService.updateStatus(1L, 99L, "DISABLED"));
        assertEquals(404, ex.getCode());
        verify(agentMapper, never()).updateStatusOnly(any(), any());
    }
}
