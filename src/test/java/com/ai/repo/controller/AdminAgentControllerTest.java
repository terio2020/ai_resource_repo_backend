package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminAgentResponse;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AdminAgentService;
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

@SpringBootTest(classes = {AdminAgentController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        ValidationAutoConfiguration.class
})
class AdminAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAgentService adminAgentService;

    @Test
    void listAgents_shouldReturnPage() throws Exception {
        AdminAgentResponse item = new AdminAgentResponse();
        item.setId(1L);
        item.setName("agent1");
        item.setStatus("ACTIVE");
        PageResult<AdminAgentResponse> page =
                new PageResult<>(Collections.singletonList(item), 1L, 1L, 10L);
        when(adminAgentService.listAgents(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/agents").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].name").value("agent1"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void updateStatus_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/agents/3/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminAgentService).updateStatus(1L, 3L, "DISABLED");
    }

    @Test
    void updateStatus_shouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/agents/3/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BANNED\"}"))
                .andExpect(status().is4xxClientError());
    }
}
