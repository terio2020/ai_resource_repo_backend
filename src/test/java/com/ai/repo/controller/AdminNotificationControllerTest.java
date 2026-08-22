package com.ai.repo.controller;

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

import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.AdminEmailService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AdminNotificationController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class, ValidationAutoConfiguration.class})
class AdminNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AdminEmailService adminEmailService;

    @Test
    void sendEmailQueuesCustomAdminEmail() throws Exception {
        mockMvc.perform(post("/api/admin/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Test\",\"body\":\"Hello admins\","
                                + "\"actionUrl\":\"/admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(adminEmailService).sendToActiveAdmins("Test", "Hello admins", "/admin");
    }

    @Test
    void sendEmailRejectsMissingBody() throws Exception {
        mockMvc.perform(post("/api/admin/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendEmailRejectsExternalActionUrl() throws Exception {
        mockMvc.perform(post("/api/admin/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Test\",\"body\":\"Body\","
                                + "\"actionUrl\":\"https://example.com/phish\"}"))
                .andExpect(status().isBadRequest());
    }
}
