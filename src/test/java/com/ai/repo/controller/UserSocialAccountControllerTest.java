package com.ai.repo.controller;

import com.ai.repo.entity.SocialAccount;
import com.ai.repo.exception.GlobalExceptionHandler;
import com.ai.repo.service.SocialAccountService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {UserSocialAccountController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        MultipartAutoConfiguration.class
})
class UserSocialAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocialAccountService socialAccountService;

    private RequestPostProcessor withUserId(Long userId) {
        return request -> {
            request.setAttribute("userId", userId);
            return request;
        };
    }

    @Test
    void getLinkedAccounts_shouldReturnAccounts() throws Exception {
        SocialAccount account = new SocialAccount();
        account.setId(1L);
        account.setUserId(1L);
        account.setProvider("google");

        when(socialAccountService.findByUserId(1L)).thenReturn(List.of(account));

        mockMvc.perform(get("/api/users/social-accounts").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].provider").value("google"));
    }

    @Test
    void unlinkSocialAccount_shouldSucceed() throws Exception {
        when(socialAccountService.unlinkSocialAccount(1L, "google")).thenReturn(true);

        mockMvc.perform(delete("/api/users/social-accounts/google").with(withUserId(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
