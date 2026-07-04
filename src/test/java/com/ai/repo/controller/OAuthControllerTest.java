package com.ai.repo.controller;

import com.ai.repo.dto.LoginResponse;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
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
@TestPropertySource(properties = {
        "oauth.google.client-id=google-client-id",
        "oauth.google.client-secret=google-secret",
        "oauth.google.redirect-uri=http://localhost:8080/api/oauth/google/callback",
        "oauth.github.client-id=github-client-id",
        "oauth.github.client-secret=github-secret",
        "oauth.github.redirect-uri=http://localhost:8080/api/oauth/github/callback",
        "oauth.state-secret=test-state-secret",
        "app.base-url=http://localhost:8080",
        "app.frontend-url=http://localhost:3000"
})
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocialAccountService socialAccountService;

    @MockBean
    private UserService userService;

    // ==================== initiateOAuth ====================

    @Test
    void initiateOAuth_shouldRedirect_whenProviderConfigured() throws Exception {
        when(socialAccountService.isProviderConfigured("google")).thenReturn(true);
        when(socialAccountService.generateState(null)).thenReturn("test-state");
        when(socialAccountService.buildAuthorizationUrl("google", "test-state"))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test-state");

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

    @Test
    void initiateOAuth_githubConfigured_shouldRedirectWithGithubAuthUrl() throws Exception {
        when(socialAccountService.isProviderConfigured("github")).thenReturn(true);
        when(socialAccountService.generateState(null)).thenReturn("test-state");
        when(socialAccountService.buildAuthorizationUrl("github", "test-state"))
                .thenReturn("https://github.com/login/oauth/authorize?state=test-state");

        mockMvc.perform(get("/api/oauth/github"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("github.com/login/oauth/authorize")));
    }

    @Test
    void initiateOAuth_withRedirectUri_shouldStillSucceed() throws Exception {
        when(socialAccountService.isProviderConfigured("google")).thenReturn(true);
        when(socialAccountService.generateState("http://myapp.com/callback")).thenReturn("test-state");
        when(socialAccountService.buildAuthorizationUrl("google", "test-state"))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test-state");

        mockMvc.perform(get("/api/oauth/google").param("redirect_uri", "http://myapp.com/callback"))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
    }

    // ==================== handleOAuthCallback — state validation ====================

    @Test
    void handleOAuthCallback_shouldReturn400_whenStateInvalid() throws Exception {
        when(socialAccountService.validateAndExtractState("bad-state")).thenReturn(null);

        mockMvc.perform(get("/api/oauth/google/callback")
                        .param("code", "auth-code")
                        .param("state", "bad-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void handleOAuthCallback_google_shouldReturn400_whenProviderCallFails() throws Exception {
        when(socialAccountService.validateAndExtractState("valid-state")).thenReturn("default");
        when(socialAccountService.exchangeCodeForUserInfo("google", "any-code")).thenReturn(null);

        mockMvc.perform(get("/api/oauth/google/callback")
                        .param("code", "any-code")
                        .param("state", "valid-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void handleOAuthCallback_github_shouldReturn400_whenProviderCallFails() throws Exception {
        when(socialAccountService.validateAndExtractState("valid-state")).thenReturn("default");
        when(socialAccountService.exchangeCodeForUserInfo("github", "any-code")).thenReturn(null);

        mockMvc.perform(get("/api/oauth/github/callback")
                        .param("code", "any-code")
                        .param("state", "valid-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void handleOAuthCallback_shouldRedirectToFrontend_whenNewUser() throws Exception {
        User newUser = new User();
        newUser.setId(1L);
        newUser.setUsername("google_12345");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(1L);
        loginResponse.setAccessToken("access-token");
        loginResponse.setRefreshToken("refresh-token");
        loginResponse.setUsername("google_12345");
        loginResponse.setNickname("Test User");
        loginResponse.setEmail("user@gmail.com");
        loginResponse.setAvatar("https://example.com/avatar.png");

        Map<String, Object> userInfo = Map.of(
                "id", "12345",
                "email", "user@gmail.com",
                "name", "Test User",
                "picture", "https://example.com/avatar.png",
                "access_token", "oauth-at",
                "refresh_token", "oauth-rt",
                "expires_in", 3600L
        );

        when(socialAccountService.validateAndExtractState("valid-state")).thenReturn("default");
        when(socialAccountService.exchangeCodeForUserInfo("google", "code-123")).thenReturn(userInfo);
        when(socialAccountService.authenticateWithSocialAccount("google", "12345")).thenReturn(null);
        when(socialAccountService.linkSocialAccountToNewUser("google", "12345", "user@gmail.com",
                "Test User", "https://example.com/avatar.png", "oauth-at", "oauth-rt", 3600L))
                .thenReturn(newUser);
        when(userService.generateTokens(1L)).thenReturn(loginResponse);

        mockMvc.perform(get("/api/oauth/google/callback")
                        .param("code", "code-123")
                        .param("state", "valid-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("http://localhost:3000/oauth/google/callback"),
                        org.hamcrest.Matchers.containsString("accessToken=access-token"),
                        org.hamcrest.Matchers.containsString("refreshToken=refresh-token"),
                        org.hamcrest.Matchers.containsString("userId=1"),
                        org.hamcrest.Matchers.containsString("username=google_12345"),
                        org.hamcrest.Matchers.containsString("nickname=Test+User"),
                        org.hamcrest.Matchers.containsString("email=user%40gmail.com")
                )));
    }

    @Test
    void handleOAuthCallback_shouldRedirectToFrontend_whenExistingUser() throws Exception {
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUsername("existing_user");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(2L);
        loginResponse.setAccessToken("access-token");
        loginResponse.setRefreshToken("refresh-token");
        loginResponse.setUsername("existing_user");
        loginResponse.setNickname("Existing User");
        loginResponse.setEmail("existing@gmail.com");
        loginResponse.setAvatar("https://example.com/avatar.png");

        Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("id", "67890");
        userInfo.put("email", "existing@gmail.com");
        userInfo.put("name", "Existing User");
        userInfo.put("picture", "https://example.com/avatar.png");
        userInfo.put("access_token", "oauth-at");
        userInfo.put("refresh_token", null);
        userInfo.put("expires_in", null);

        com.ai.repo.entity.SocialAccount socialAccount = new com.ai.repo.entity.SocialAccount();
        socialAccount.setId(10L);
        socialAccount.setUserId(2L);

        when(socialAccountService.validateAndExtractState("valid-state")).thenReturn("default");
        when(socialAccountService.exchangeCodeForUserInfo("github", "code-456")).thenReturn(userInfo);
        when(socialAccountService.authenticateWithSocialAccount("github", "67890")).thenReturn(existingUser);
        when(socialAccountService.findByUserIdAndProvider(2L, "github")).thenReturn(socialAccount);
        when(userService.generateTokens(2L)).thenReturn(loginResponse);

        mockMvc.perform(get("/api/oauth/github/callback")
                        .param("code", "code-456")
                        .param("state", "valid-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("http://localhost:3000/oauth/github/callback"),
                        org.hamcrest.Matchers.containsString("accessToken=access-token"),
                        org.hamcrest.Matchers.containsString("userId=2")
                )));
    }
}
