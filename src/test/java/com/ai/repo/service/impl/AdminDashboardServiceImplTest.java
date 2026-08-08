package com.ai.repo.service.impl;

import com.ai.repo.dto.AdminOverviewResponse;
import com.ai.repo.dto.AgentStatusCount;
import com.ai.repo.dto.ActivityTrendItem;
import com.ai.repo.dto.DailyCount;
import com.ai.repo.dto.DownloadTrendItem;
import com.ai.repo.dto.TopAgentItem;
import com.ai.repo.mapper.AdminDashboardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private AdminDashboardMapper adminDashboardMapper;

    private AdminDashboardServiceImpl adminDashboardService;

    @BeforeEach
    void setUp() throws Exception {
        adminDashboardService = new AdminDashboardServiceImpl();
        Field mapperField = AdminDashboardServiceImpl.class.getDeclaredField("adminDashboardMapper");
        mapperField.setAccessible(true);
        mapperField.set(adminDashboardService, adminDashboardMapper);
    }

    @Test
    void getOverview_shouldReturnMapperResult() {
        AdminOverviewResponse expected = new AdminOverviewResponse();
        expected.setUserCount(10L);
        expected.setAgentCount(5L);
        expected.setActiveAgentCount(3L);
        when(adminDashboardMapper.selectOverview()).thenReturn(expected);

        AdminOverviewResponse result = adminDashboardService.getOverview();

        assertSame(expected, result);
        verify(adminDashboardMapper).selectOverview();
    }

    @Test
    void getUserGrowth_shouldFillMissingDatesWithZero() {
        LocalDate today = LocalDate.now();
        LocalDate day2 = today.minusDays(2);
        DailyCount row = new DailyCount();
        row.setDate(day2);
        row.setCount(3L);
        when(adminDashboardMapper.selectUserGrowth(any())).thenReturn(Collections.singletonList(row));

        List<DailyCount> result = adminDashboardService.getUserGrowth(7);

        assertEquals(7, result.size());
        assertEquals(today.minusDays(6), result.get(0).getDate());
        assertEquals(3L, result.get(4).getCount());
        assertEquals(0L, result.get(0).getCount());
        assertEquals(0L, result.get(6).getCount());
    }

    @Test
    void getUserGrowth_shouldPassStartDate() {
        when(adminDashboardMapper.selectUserGrowth(any())).thenReturn(Collections.emptyList());
        adminDashboardService.getUserGrowth(30);
        LocalDate expectedStart = LocalDate.now().minusDays(29);
        verify(adminDashboardMapper).selectUserGrowth(expectedStart);
    }

    @Test
    void getActivity_shouldFillMissingDatesWithZero() {
        LocalDate today = LocalDate.now();
        LocalDate day3 = today.minusDays(3);
        ActivityTrendItem row = new ActivityTrendItem();
        row.setDate(day3);
        row.setLogins(1L);
        row.setMemories(2L);
        row.setAgents(3L);
        when(adminDashboardMapper.selectActivity(any())).thenReturn(Collections.singletonList(row));

        List<ActivityTrendItem> result = adminDashboardService.getActivity(7);

        assertEquals(7, result.size());
        ActivityTrendItem filled = result.get(3);
        assertEquals(day3, filled.getDate());
        assertEquals(1L, filled.getLogins());
        assertEquals(2L, filled.getMemories());
        assertEquals(3L, filled.getAgents());
        assertEquals(0L, result.get(0).getLogins());
        assertEquals(0L, result.get(0).getMemories());
        assertEquals(0L, result.get(0).getAgents());
    }

    @Test
    void getDownloads_shouldFillMissingDatesWithZero() {
        LocalDate today = LocalDate.now();
        LocalDate day2 = today.minusDays(2);
        DownloadTrendItem row = new DownloadTrendItem();
        row.setDate(day2);
        row.setMemoryDownloads(4L);
        row.setPackageDownloads(5L);
        row.setRepoDownloads(6L);
        when(adminDashboardMapper.selectDownloads(any())).thenReturn(Collections.singletonList(row));

        List<DownloadTrendItem> result = adminDashboardService.getDownloads(7);

        assertEquals(7, result.size());
        DownloadTrendItem filled = result.get(4);
        assertEquals(day2, filled.getDate());
        assertEquals(4L, filled.getMemoryDownloads());
        assertEquals(5L, filled.getPackageDownloads());
        assertEquals(6L, filled.getRepoDownloads());
        assertEquals(0L, result.get(0).getMemoryDownloads());
        assertEquals(0L, result.get(0).getPackageDownloads());
        assertEquals(0L, result.get(0).getRepoDownloads());
    }

    @Test
    void getAgentStatusDistribution_shouldReturnMapperResult() {
        AgentStatusCount active = new AgentStatusCount();
        active.setStatus("ACTIVE");
        active.setCount(4L);
        AgentStatusCount disabled = new AgentStatusCount();
        disabled.setStatus("DISABLED");
        disabled.setCount(1L);
        when(adminDashboardMapper.selectAgentStatusDistribution()).thenReturn(Arrays.asList(active, disabled));

        List<AgentStatusCount> result = adminDashboardService.getAgentStatusDistribution();

        assertEquals(2, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
        assertEquals(4L, result.get(0).getCount());
        assertEquals("DISABLED", result.get(1).getStatus());
    }

    @Test
    void getTopAgents_shouldReturnMapperResult() {
        TopAgentItem item = new TopAgentItem();
        item.setAgentId(1L);
        item.setAgentName("agent-1");
        item.setMemoryCount(9L);
        item.setLikeCount(7L);
        item.setDownloadCount(5L);
        when(adminDashboardMapper.selectTopAgents(10)).thenReturn(Collections.singletonList(item));

        List<TopAgentItem> result = adminDashboardService.getTopAgents(10);

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).getMemoryCount());
        verify(adminDashboardMapper).selectTopAgents(10);
    }
}
