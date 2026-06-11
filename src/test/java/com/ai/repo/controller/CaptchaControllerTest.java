package com.ai.repo.controller;

import com.ai.repo.dto.CaptchaVerifyRequest;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.CaptchaService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {CaptchaController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class CaptchaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CaptchaService captchaService;

    @Test
    void generateCaptcha_shouldReturnCaptchaResponse() throws Exception {
        CaptchaService.CaptchaResult result = new CaptchaService.CaptchaResult(
                "captcha-1", "data:image/png;base64,abc123", 100);

        when(captchaService.generateCaptcha()).thenReturn(result);

        mockMvc.perform(get("/api/captcha/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("captcha-1"))
                .andExpect(jsonPath("$.data.puzzleImage").value("data:image/png;base64,abc123"));
    }

    @Test
    void verifyCaptcha_shouldReturnTrue_whenCorrect() throws Exception {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setId("captcha-1");
        request.setMoveX(100);

        when(captchaService.verifyCaptcha("captcha-1", 100)).thenReturn(true);

        mockMvc.perform(post("/api/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void verifyCaptcha_shouldReturnFalse_whenWrong() throws Exception {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setId("captcha-1");
        request.setMoveX(200);

        when(captchaService.verifyCaptcha("captcha-1", 200)).thenReturn(false);

        mockMvc.perform(post("/api/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }
}
