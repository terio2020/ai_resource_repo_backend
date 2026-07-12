package com.ai.repo.controller;

import com.ai.repo.entity.BugReport;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.BugReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {BugReportController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class
})
class BugReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BugReportService bugReportService;

    private RequestPostProcessor withAgentId(Long agentId) {
        return request -> {
            request.setAttribute("agentId", agentId);
            return request;
        };
    }

    private BugReport buildBugReport(Long id, Long agentId, String title) {
        BugReport bug = new BugReport();
        bug.setId(id);
        bug.setUid("uid" + id);
        bug.setAgentId(agentId);
        bug.setTitle(title);
        bug.setDescription("Description for " + title);
        bug.setSeverity("error");
        bug.setStatus("open");
        bug.setCreatedAt(LocalDateTime.now());
        bug.setUpdatedAt(LocalDateTime.now());
        return bug;
    }

    // ==================== POST /api/bugs ====================

    @Test
    void createBugReport_shouldSucceed() throws Exception {
        BugReport bug = buildBugReport(1L, 5L, "API returns 500");
        when(bugReportService.create(any(BugReport.class))).thenReturn(bug);

        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"API returns 500\",\"severity\":\"error\",\"description\":\"The endpoint crashes\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("API returns 500"))
                .andExpect(jsonPath("$.data.status").value("open"));

        verify(bugReportService).create(any(BugReport.class));
    }

    @Test
    void createBugReport_withAllFields_shouldSucceed() throws Exception {
        BugReport bug = buildBugReport(1L, 5L, "Full bug report");
        bug.setSeverity("critical");
        bug.setSource("POST /api/skill-repos");
        bug.setCategory("api");
        when(bugReportService.create(any(BugReport.class))).thenReturn(bug);

        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Full bug report\",\"severity\":\"critical\",\"source\":\"POST /api/skill-repos\",\"category\":\"api\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.severity").value("critical"))
                .andExpect(jsonPath("$.data.source").value("POST /api/skill-repos"))
                .andExpect(jsonPath("$.data.category").value("api"));
    }

    // ==================== GET /api/bugs/{id} ====================

    @Test
    void getBugReportById_shouldReturnBugReport() throws Exception {
        BugReport bug = buildBugReport(1L, 5L, "Test bug");
        when(bugReportService.findById(1L)).thenReturn(bug);

        mockMvc.perform(get("/api/bugs/1")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test bug"));
    }

    @Test
    void getBugReportById_notFound_shouldReturnError() throws Exception {
        when(bugReportService.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/bugs/999")
                        .with(withAgentId(5L)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Bug report not found"));
    }

    // ==================== GET /api/bugs/uid/{uid} ====================

    @Test
    void getBugReportByUid_shouldReturnBugReport() throws Exception {
        BugReport bug = buildBugReport(1L, 5L, "Test bug");
        bug.setUid("abc123");
        when(bugReportService.findByUid("abc123")).thenReturn(bug);

        mockMvc.perform(get("/api/bugs/uid/abc123")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uid").value("abc123"));
    }

    @Test
    void getBugReportByUid_notFound_shouldReturnError() throws Exception {
        when(bugReportService.findByUid("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/bugs/uid/nonexistent")
                        .with(withAgentId(5L)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Bug report not found"));
    }

    // ==================== GET /api/bugs ====================

    @Test
    void listBugReports_noFilters_shouldReturnAll() throws Exception {
        List<BugReport> bugs = Arrays.asList(
                buildBugReport(1L, 5L, "Bug 1"),
                buildBugReport(2L, 6L, "Bug 2")
        );
        when(bugReportService.findWithFilters(null, null, null, null)).thenReturn(bugs);

        mockMvc.perform(get("/api/bugs")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void listBugReports_withFilters_shouldReturnFiltered() throws Exception {
        List<BugReport> bugs = Arrays.asList(buildBugReport(1L, 5L, "Critical bug"));
        when(bugReportService.findWithFilters(5L, "critical", "open", "api")).thenReturn(bugs);

        mockMvc.perform(get("/api/bugs")
                        .param("agentId", "5")
                        .param("severity", "critical")
                        .param("status", "open")
                        .param("category", "api")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void listBugReports_empty_shouldReturnEmptyList() throws Exception {
        when(bugReportService.findWithFilters(null, null, null, null)).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/bugs")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ==================== GET /api/bugs/agent/{agentId} ====================

    @Test
    void getBugReportsByAgent_shouldReturnList() throws Exception {
        List<BugReport> bugs = Arrays.asList(
                buildBugReport(1L, 5L, "Bug by agent 5")
        );
        when(bugReportService.findByAgentId(5L)).thenReturn(bugs);

        mockMvc.perform(get("/api/bugs/agent/5")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].agentId").value(5));
    }

    // ==================== PUT /api/bugs/{id} ====================

    @Test
    void updateBugReport_shouldSucceed() throws Exception {
        BugReport existing = buildBugReport(1L, 5L, "Old title");
        BugReport updated = buildBugReport(1L, 5L, "Updated title");
        when(bugReportService.findById(1L)).thenReturn(existing);
        when(bugReportService.update(any(BugReport.class))).thenReturn(updated);

        mockMvc.perform(put("/api/bugs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Updated title"));
    }

    @Test
    void updateBugReport_notFound_shouldReturnError() throws Exception {
        when(bugReportService.findById(999L)).thenReturn(null);

        mockMvc.perform(put("/api/bugs/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"test\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Bug report not found"));
    }

    @Test
    void updateBugReport_wrongAgent_shouldReturnForbidden() throws Exception {
        BugReport existing = buildBugReport(1L, 10L, "Other agent's bug");
        when(bugReportService.findById(1L)).thenReturn(existing);

        mockMvc.perform(put("/api/bugs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Only the reporting agent can update this bug report"));
    }

    // ==================== PATCH /api/bugs/{id}/status ====================

    @Test
    void updateBugReportStatus_shouldSucceed() throws Exception {
        BugReport existing = buildBugReport(1L, 5L, "Test bug");
        when(bugReportService.findById(1L)).thenReturn(existing);
        when(bugReportService.updateStatus(1L, "resolved")).thenReturn(true);

        mockMvc.perform(patch("/api/bugs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"resolved\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateBugReportStatus_notFound_shouldReturnError() throws Exception {
        when(bugReportService.findById(999L)).thenReturn(null);

        mockMvc.perform(patch("/api/bugs/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"resolved\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Bug report not found"));
    }

    @Test
    void updateBugReportStatus_wrongAgent_shouldReturnForbidden() throws Exception {
        BugReport existing = buildBugReport(1L, 10L, "Other agent's bug");
        when(bugReportService.findById(1L)).thenReturn(existing);

        mockMvc.perform(patch("/api/bugs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"resolved\"}")
                        .with(withAgentId(5L)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Only the reporting agent can update this bug report"));
    }

    // ==================== DELETE /api/bugs/{id} ====================

    @Test
    void deleteBugReport_shouldSucceed() throws Exception {
        BugReport existing = buildBugReport(1L, 5L, "Test bug");
        when(bugReportService.findById(1L)).thenReturn(existing);
        when(bugReportService.delete(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/bugs/1")
                        .with(withAgentId(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteBugReport_notFound_shouldReturnError() throws Exception {
        when(bugReportService.findById(999L)).thenReturn(null);

        mockMvc.perform(delete("/api/bugs/999")
                        .with(withAgentId(5L)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Bug report not found"));
    }

    @Test
    void deleteBugReport_wrongAgent_shouldReturnForbidden() throws Exception {
        BugReport existing = buildBugReport(1L, 10L, "Other agent's bug");
        when(bugReportService.findById(1L)).thenReturn(existing);

        mockMvc.perform(delete("/api/bugs/1")
                        .with(withAgentId(5L)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Only the reporting agent can delete this bug report"));
    }
}
