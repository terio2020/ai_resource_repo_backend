package com.ai.repo.service;

import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.dto.AgentStatsResponse;
import com.ai.repo.dto.AgentSyncResponse;
import com.ai.repo.entity.Agent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AgentServiceTest {

    @Autowired
    private AgentService agentService;

    @Test
    public void testFindPage() {
        var pageResult = agentService.findPage(1, 10);
        assertNotNull(pageResult);
        assertNotNull(pageResult.getRecords());
        assertTrue(pageResult.getRecords().size() <= 10);
    }

    @Test
    public void testFindBySearch() {
        AgentSearchRequest request = new AgentSearchRequest();
        request.setName("test");
        request.setPage(1);
        request.setSize(10);
        
        List<Agent> agents = agentService.findBySearch(request);
        assertNotNull(agents);
    }

    @Test
    public void testUpdateHeartbeat() {
        Agent agent = new Agent();
        agent.setUserId(1L);
        agent.setName("Test Agent");
        agent.setCode("test-heartbeat-" + System.currentTimeMillis());
        agent.setStatus("OFFLINE");
        agent.setType("test");
        agent.setSyncEnabled(false);
        
        Agent created = agentService.create(agent);
        assertNotNull(created.getId());
        
        boolean updated = agentService.updateHeartbeat(created.getId(), "ONLINE", java.time.LocalDateTime.now().toString());
        assertTrue(updated);
    }

    @Test
    public void testUpdateStatusOnly() {
        Agent agent = new Agent();
        agent.setUserId(1L);
        agent.setName("Test Agent");
        agent.setCode("test-status-" + System.currentTimeMillis());
        agent.setStatus("OFFLINE");
        agent.setType("test");
        agent.setSyncEnabled(false);
        
        Agent created = agentService.create(agent);
        assertNotNull(created.getId());
        
        boolean updated = agentService.updateStatusOnly(created.getId(), "BUSY");
        assertTrue(updated);
    }

    @Test
    public void testUpdateConfigOnly() {
        Agent agent = new Agent();
        agent.setUserId(1L);
        agent.setName("Test Agent");
        agent.setCode("test-config-" + System.currentTimeMillis());
        agent.setStatus("OFFLINE");
        agent.setType("test");
        agent.setSyncEnabled(false);
        
        Agent created = agentService.create(agent);
        assertNotNull(created.getId());
        
        boolean updated = agentService.updateConfigOnly(created.getId(), "{\"test\": true}");
        assertTrue(updated);
    }

    @Test
    public void testSyncData() {
        Agent agent = new Agent();
        agent.setUserId(1L);
        agent.setName("Test Agent");
        agent.setCode("test-sync-" + System.currentTimeMillis());
        agent.setStatus("OFFLINE");
        agent.setType("test");
        agent.setSyncEnabled(false);
        
        Agent created = agentService.create(agent);
        assertNotNull(created.getId());
        
        AgentSyncResponse syncResponse = agentService.syncData(created.getId(), null);
        assertNotNull(syncResponse);
        assertNotNull(syncResponse.getSkills());
        assertNotNull(syncResponse.getMemories());
        assertNotNull(syncResponse.getSyncTime());
    }
}