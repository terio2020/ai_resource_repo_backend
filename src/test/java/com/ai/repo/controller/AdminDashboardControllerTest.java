package com.ai.repo.controller;

import com.ai.repo.dto.AdminOverviewResponse;
import com.ai.repo.dto.AgentStatusCount;
import com.ai.repo.dto.ActivityTrendItem;
import com.ai.repo.dto.DailyCount;
import com.ai.repo.dto.DownloadTrendItem;
import com.ai.repo.dto.TopAgentItem;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {AdminDashboardController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        ValidationAutoConfiguration.class
})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @Test
    void getOverview_shouldReturn200WithData() throws Exception {
        AdminOverviewResponse overview = new AdminOverviewResponse();
        overview.setUserCount(10L);
        overview.setAgentCount(5L);
        overview.setActiveAgentCount(3L);
        when(adminDashboardService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userCount").value(10))
                .andExpect(jsonPath("$.data.agentCount").value(5))
                .andExpect(jsonPath("$.data.activeAgentCount").value(3));
    }

    @Test
    void getUserGrowth_shouldReturnFilledSeries() throws Exception {
        DailyCount item = new DailyCount();
        item.setDate(LocalDate.now().minusDays(1));
        item.setCount(2L);
        when(adminDashboardService.getUserGrowth(anyInt())).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/admin/dashboard/user-growth").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].count").value(2));
    }

    @Test
    void getUserGrowth_shouldRejectInvalidDays() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/user-growth").param("days", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getActivity_shouldReturnData() throws Exception {
        ActivityTrendItem item = new ActivityTrendItem();
        item.setDate(LocalDate.now());
        item.setLogins(1L);
        item.setMemories(2L);
        item.setAgents(3L);
        when(adminDashboardService.getActivity(anyInt())).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/admin/dashboard/activity").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].logins").value(1))
                .andExpect(jsonPath("$.data[0].memories").value(2))
                .andExpect(jsonPath("$.data[0].agents").value(3));
    }

    @Test
    void getDownloads_shouldReturnData() throws Exception {
        DownloadTrendItem item = new DownloadTrendItem();
        item.setDate(LocalDate.now());
        item.setMemoryDownloads(4L);
        item.setPackageDownloads(5L);
        item.setRepoDownloads(6L);
        when(adminDashboardService.getDownloads(anyInt())).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/admin/dashboard/downloads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memoryDownloads").value(4))
                .andExpect(jsonPath("$.data[0].packageDownloads").value(5))
                .andExpect(jsonPath("$.data[0].repoDownloads").value(6));
    }

    @Test
    void getAgentStatusDistribution_shouldReturnData() throws Exception {
        AgentStatusCount item = new AgentStatusCount();
        item.setStatus("ACTIVE");
        item.setCount(4L);
        when(adminDashboardService.getAgentStatusDistribution()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/admin/dashboard/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].count").value(4));
    }

    @Test
    void getTopAgents_shouldReturnData() throws Exception {
        TopAgentItem item = new TopAgentItem();
        item.setAgentId(1L);
        item.setAgentName("agent-1");
        item.setMemoryCount(9L);
        item.setLikeCount(7L);
        item.setDownloadCount(5L);
        when(adminDashboardService.getTopAgents(anyInt())).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/admin/dashboard/top-agents").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].agentName").value("agent-1"))
                .andExpect(jsonPath("$.data[0].memoryCount").value(9));
    }

    @Test
    void getTopAgents_shouldRejectInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/top-agents").param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }
}
