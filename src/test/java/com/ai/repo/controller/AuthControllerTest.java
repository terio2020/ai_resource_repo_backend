package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.TempTokenGetResponse;
import com.ai.repo.dto.TempTokenStoreRequest;
import com.ai.repo.dto.TempTokenStoreResponse;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.TempTokenService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TempTokenService tempTokenService;

    private RequestPostProcessor withUserId(Long userId) {
        return request -> {
            request.setAttribute("userId", userId);
            return request;
        };
    }

    @Test
    void storeTempToken_shouldReturnSessionId() throws Exception {
        TempTokenStoreRequest request = new TempTokenStoreRequest();
        request.setSessionId("session-123");
        request.setAccessToken("token-abc");

        when(tempTokenService.storeToken("session-123", "token-abc")).thenReturn("session-123");

        mockMvc.perform(post("/api/auth/temp-token")
                        .with(withUserId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("session-123"));
    }

    @Test
    void getTempToken_shouldReturnToken_whenFound() throws Exception {
        when(tempTokenService.getAndRemoveToken("session-123")).thenReturn("token-abc");

        mockMvc.perform(get("/api/auth/temp-token/session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("token-abc"));
    }

    @Test
    void getTempToken_shouldReturn404_whenNotFound() throws Exception {
        when(tempTokenService.getAndRemoveToken("session-456")).thenReturn(null);

        mockMvc.perform(get("/api/auth/temp-token/session-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Token not found or expired"));
    }
}
