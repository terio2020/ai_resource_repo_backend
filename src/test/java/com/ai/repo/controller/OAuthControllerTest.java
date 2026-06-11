package com.ai.repo.controller;

import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.SocialAccountService;
import com.ai.repo.service.UserService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {OAuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocialAccountService socialAccountService;

    @MockBean
    private UserService userService;

    @Test
    void initiateOAuth_shouldRedirect_whenProviderConfigured() throws Exception {
        when(socialAccountService.isProviderConfigured("google")).thenReturn(true);

        mockMvc.perform(get("/api/oauth/google"))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
    }

    @Test
    void initiateOAuth_shouldReturn400_whenProviderNotConfigured() throws Exception {
        when(socialAccountService.isProviderConfigured("unknown")).thenReturn(false);

        mockMvc.perform(get("/api/oauth/unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
