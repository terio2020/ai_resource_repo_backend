package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.entity.BugReport;
import com.ai.repo.entity.Memory;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AdminContentService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AdminContentController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        ValidationAutoConfiguration.class
})
class AdminContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminContentService adminContentService;

    @Test
    void updateMemoryStatus_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/content/memories/1/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BANNED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminContentService).updateMemoryStatus(1L, 1L, "BANNED");
    }

    @Test
    void listMemories_shouldReturnPage() throws Exception {
        Memory item = new Memory();
        item.setId(8L);
        item.setTitle("Moderated memory");
        item.setStatus("BANNED");
        PageResult<Memory> page = new PageResult<>(Collections.singletonList(item), 1L, 1L, 10L);
        when(adminContentService.listMemories(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/content/memories")
                        .param("page", "1").param("size", "10").param("status", "BANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].title").value("Moderated memory"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void updateMemoryStatus_shouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/content/memories/1/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELETED\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateSkillRepoStatus_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/content/skill-repos/2/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BANNED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminContentService).updateSkillRepoStatus(1L, 2L, "BANNED");
    }

    @Test
    void updatePackageStatus_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/content/packages/3/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VISIBLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminContentService).updatePackageStatus(1L, 3L, "VISIBLE");
    }

    @Test
    void listBugReports_shouldReturnPage() throws Exception {
        BugReport item = new BugReport();
        item.setId(1L);
        item.setTitle("Bug title");
        item.setStatus("open");
        PageResult<BugReport> page =
                new PageResult<>(Collections.singletonList(item), 1L, 1L, 10L);
        when(adminContentService.listBugReports(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/content/bug-reports")
                        .param("page", "1").param("size", "10")
                        .param("status", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].status").value("open"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getBugReport_shouldReturnDetail() throws Exception {
        BugReport report = new BugReport();
        report.setId(6L);
        report.setTitle("Admin-only bug");
        when(adminContentService.getBugReport(6L)).thenReturn(report);

        mockMvc.perform(get("/api/admin/content/bug-reports/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Admin-only bug"));
    }

    @Test
    void updateBugReportStatus_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/content/bug-reports/4/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"resolved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminContentService).updateBugReportStatus(1L, 4L, "resolved");
    }

    @Test
    void updateBugReportStatus_shouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/content/bug-reports/4/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"deleted\"}"))
                .andExpect(status().is4xxClientError());
    }
}
