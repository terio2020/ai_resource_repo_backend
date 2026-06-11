package com.ai.repo.controller;

import com.ai.repo.entity.VerificationChallenge;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.VerifyChallengeService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {VerifyChallengeController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class VerifyChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VerifyChallengeService verifyChallengeService;

    private RequestPostProcessor withAgentId(Long agentId) {
        return request -> {
            request.setAttribute("agentId", agentId);
            return request;
        };
    }

    @Test
    void requestChallenge_shouldReturnChallenge() throws Exception {
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setVerificationCode("code-123");
        challenge.setChallengeText("12 + 34 = ?");
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setMaxAttempts(3);

        when(verifyChallengeService.requestChallenge(1L)).thenReturn(challenge);

        mockMvc.perform(get("/api/auth/challenge").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verificationCode").value("code-123"));
    }

    @Test
    void verifyAnswer_shouldReturnVerified_whenCorrect() throws Exception {
        when(verifyChallengeService.verifyAnswer(anyString(), any(), anyLong())).thenReturn(true);

        Map<String, Object> body = Map.of("verificationCode", "code-123", "answer", 46);

        mockMvc.perform(post("/api/auth/challenge/verify")
                        .with(withAgentId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    void verifyAnswer_shouldReturnNotVerified_whenWrong() throws Exception {
        when(verifyChallengeService.verifyAnswer(anyString(), any(), anyLong())).thenReturn(false);

        Map<String, Object> body = Map.of("verificationCode", "code-123", "answer", 99);

        mockMvc.perform(post("/api/auth/challenge/verify")
                        .with(withAgentId(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verified").value(false));
    }

    @Test
    void checkStatus_shouldReturnNotLockedOut() throws Exception {
        when(verifyChallengeService.isLockedOut(1L)).thenReturn(false);

        mockMvc.perform(get("/api/auth/challenge/status").with(withAgentId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.lockedOut").value(false));
    }
}
