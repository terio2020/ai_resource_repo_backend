package com.ai.repo.service.impl;

import com.ai.repo.dto.HomeData;
import com.ai.repo.entity.Agent;
import com.ai.repo.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock
    private AgentService agentService;

    private HomeServiceImpl homeService;

    @BeforeEach
    void setUp() throws Exception {
        homeService = new HomeServiceImpl();
        Field field = HomeServiceImpl.class.getDeclaredField("agentService");
        field.setAccessible(true);
        field.set(homeService, agentService);
    }

    private Agent sampleAgent() {
        Agent a = new Agent();
        a.setId(1L);
        a.setName("test-agent");
        a.setDisplayName("Test Agent");
        a.setDescription("A test agent");
        a.setKarma(10);
        return a;
    }

    @Test
    void getHome_shouldReturnHomeData_whenAgentFound() {
        when(agentService.findById(1L)).thenReturn(sampleAgent());

        HomeData result = homeService.getHome(1L);

        assertNotNull(result);
        assertNotNull(result.getYourAccount());
        assertEquals(1L, result.getYourAccount().getId());
        assertEquals("test-agent", result.getYourAccount().getName());
        assertEquals("Test Agent", result.getYourAccount().getDisplayName());
        assertEquals("A test agent", result.getYourAccount().getDescription());
        assertEquals(10, result.getYourAccount().getKarma());

        assertNotNull(result.getYourDirectMessages());
        assertEquals(0, result.getYourDirectMessages().getPendingRequestCount());
        assertEquals(0, result.getYourDirectMessages().getUnreadMessageCount());

        assertNull(result.getLatestAnnouncement());

        assertNotNull(result.getExplore());
        assertEquals("Explore the platform...", result.getExplore().getDescription());

        assertNotNull(result.getWhatToDoNext());
        assertFalse(result.getWhatToDoNext().isEmpty());

        assertNotNull(result.getQuickLinks());
        assertEquals("GET /api/notifications", result.getQuickLinks().getNotifications());
        assertEquals("GET /api/feed", result.getQuickLinks().getFeed());
        assertEquals("GET /api/agents/me", result.getQuickLinks().getMyProfile());
    }

    @Test
    void getHome_shouldReturnNull_whenAgentNotFound() {
        when(agentService.findById(999L)).thenReturn(null);

        HomeData result = homeService.getHome(999L);

        assertNull(result);
    }
}
