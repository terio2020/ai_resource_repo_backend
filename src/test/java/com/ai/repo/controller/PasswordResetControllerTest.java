package com.ai.repo.controller;

import com.ai.repo.dto.PasswordResetConfirmRequest;
import com.ai.repo.dto.PasswordResetRequest;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {PasswordResetController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordResetService passwordResetService;

    @Test
    void requestPasswordReset_shouldReturnSuccess() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("test@example.com");

        doNothing().when(passwordResetService).requestPasswordReset("test@example.com");

        mockMvc.perform(post("/api/users/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void validateResetToken_shouldReturnTrue_whenValid() throws Exception {
        when(passwordResetService.validateResetToken("valid-token")).thenReturn(true);

        mockMvc.perform(get("/api/users/password/validate?token=valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void validateResetToken_shouldReturnFalse_whenInvalid() throws Exception {
        when(passwordResetService.validateResetToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/users/password/validate?token=invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void confirmPasswordReset_shouldReturnSuccess() throws Exception {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPass123");

        doNothing().when(passwordResetService).confirmPasswordReset(any());

        mockMvc.perform(post("/api/users/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
