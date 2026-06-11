package com.ai.repo.scheduler;

import com.ai.repo.entity.Agent;
import com.ai.repo.mapper.AgentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentHeartbeatSchedulerTest {

    @Mock
    private AgentMapper agentMapper;

    private AgentHeartbeatScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        scheduler = new AgentHeartbeatScheduler();
        java.lang.reflect.Field field = AgentHeartbeatScheduler.class.getDeclaredField("agentMapper");
        field.setAccessible(true);
        field.set(scheduler, agentMapper);
    }

    @Test
    void checkOfflineAgents_shouldUpdateOfflineStatus() {
        Agent agent1 = new Agent();
        agent1.setId(1L);
        agent1.setStatus("ONLINE");

        Agent agent2 = new Agent();
        agent2.setId(2L);
        agent2.setStatus("BUSY");

        when(agentMapper.findByLastHeartbeatBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(agent1, agent2));

        scheduler.checkOfflineAgents();

        verify(agentMapper).updateAgentStatus(1L, "OFFLINE");
        verify(agentMapper).updateAgentStatus(2L, "OFFLINE");
    }

    @Test
    void checkOfflineAgents_shouldSkipAlreadyOffline() {
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setStatus("OFFLINE");

        when(agentMapper.findByLastHeartbeatBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(agent));

        scheduler.checkOfflineAgents();

        verify(agentMapper, never()).updateAgentStatus(anyLong(), anyString());
    }

    @Test
    void checkOfflineAgents_shouldHandleEmptyList() {
        when(agentMapper.findByLastHeartbeatBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.checkOfflineAgents();

        verify(agentMapper, never()).updateAgentStatus(anyLong(), anyString());
    }
}
