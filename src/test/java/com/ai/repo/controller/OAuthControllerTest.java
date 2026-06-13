package com.ai.repo.controller;

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

import java.lang.reflect.Method;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
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
        "app.base-url=http://localhost:8080"
})
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuthController controller;

    @MockBean
    private SocialAccountService socialAccountService;

    @MockBean
    private UserService userService;

    // ==================== initiateOAuth ====================

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

    @Test
    void initiateOAuth_githubConfigured_shouldRedirectWithGithubAuthUrl() throws Exception {
        when(socialAccountService.isProviderConfigured("github")).thenReturn(true);

        mockMvc.perform(get("/api/oauth/github"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("github.com/login/oauth/authorize")));
    }

    @Test
    void initiateOAuth_withRedirectUri_shouldStillSucceed() throws Exception {
        when(socialAccountService.isProviderConfigured("google")).thenReturn(true);

        mockMvc.perform(get("/api/oauth/google").param("redirect_uri", "http://myapp.com/callback"))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
    }

    // ==================== handleOAuthCallback — state validation ====================

    @Test
    void handleOAuthCallback_shouldReturn400_whenStateNotBase64() throws Exception {
        mockMvc.perform(get("/api/oauth/google/callback")
                        .param("code", "auth-code")
                        .param("state", "!!!not-valid-base64!!!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void handleOAuthCallback_shouldReturn400_whenStateHasNoColon() throws Exception {
        // Base64 of a single word with no ":" separator — fails length check
        String tooShort = Base64.getUrlEncoder().withoutPadding().encodeToString("nocolon".getBytes());
        mockMvc.perform(get("/api/oauth/google/callback")
                        .param("code", "auth-code")
                        .param("state", tooShort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * Covers the full callback path that ends up calling the real provider HTTP endpoint.
     * The HTTP exchange will fail (no real OAuth server), exercising the
     * "Failed to authenticate with X" branch which returns 400.
     */
    @Test
    void handleOAuthCallback_google_shouldReturn400_whenProviderCallFails() throws Exception {
        String state = invokeGenerateState("default");

        mockMvc.perform(get("/api/oauth/google/callback")
                        .param("code", "any-code")
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void handleOAuthCallback_github_shouldReturn400_whenProviderCallFails() throws Exception {
        String state = invokeGenerateState("default");

        mockMvc.perform(get("/api/oauth/github/callback")
                        .param("code", "any-code")
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== Private helper methods (via reflection) ====================

    private Object invokePrivate(String name, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] == null ? String.class : args[i].getClass();
        }
        Method m = OAuthController.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    private String invokeGenerateState(String redirectUri) throws Exception {
        return (String) invokePrivate("generateState", redirectUri);
    }

    @Test
    void buildAuthorizationUrl_google_shouldIncludeAllRequiredParams() throws Exception {
        String url = (String) invokePrivate("buildAuthorizationUrl", "google", "test-state");

        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
        assertTrue(url.contains("client_id=google-client-id"));
        assertTrue(url.contains("redirect_uri="));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("scope=openid"));
        assertTrue(url.contains("state=test-state"));
        assertTrue(url.contains("access_type=offline"));
    }

    @Test
    void buildAuthorizationUrl_github_shouldIncludeAllRequiredParams() throws Exception {
        String url = (String) invokePrivate("buildAuthorizationUrl", "github", "test-state");

        assertTrue(url.startsWith("https://github.com/login/oauth/authorize?"));
        assertTrue(url.contains("client_id=github-client-id"));
        assertTrue(url.contains("redirect_uri="));
        assertTrue(url.contains("scope=user:email"));
        assertTrue(url.contains("state=test-state"));
    }

    @Test
    void buildAuthorizationUrl_unknownProvider_shouldThrow() {
        Exception ex = assertThrows(Exception.class,
                () -> invokePrivate("buildAuthorizationUrl", "facebook", "state"));
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        assertTrue(cause instanceof BusinessException, "Expected BusinessException, got: " + cause);
        assertTrue(cause.getMessage().contains("Unsupported OAuth provider"));
    }

    @Test
    void generateState_shouldEmbedRedirectUriTimestampAndRandom() throws Exception {
        String state = invokeGenerateState("myapp-callback");
        assertNotNull(state);

        String decoded = new String(Base64.getUrlDecoder().decode(state));
        assertTrue(decoded.startsWith("myapp-callback:"));
        String[] parts = decoded.split(":");
        assertTrue(parts.length >= 3, "state should have at least 3 colon-separated parts");
        assertTrue(parts[parts.length - 2].matches("\\d+"), "second-to-last part should be a numeric timestamp");
        assertFalse(parts[parts.length - 1].isEmpty(), "last part should be a non-empty random uuid");
    }

    @Test
    void generateState_shouldDefaultRedirectUriWhenNull() throws Exception {
        String state = invokeGenerateState(null);
        String decoded = new String(Base64.getUrlDecoder().decode(state));
        assertTrue(decoded.startsWith("default:"));
    }

    @Test
    void generateState_shouldDefaultRedirectUriWhenEmpty() throws Exception {
        String state = invokeGenerateState("");
        String decoded = new String(Base64.getUrlDecoder().decode(state));
        assertTrue(decoded.startsWith("default:"));
    }

    @Test
    void generateState_shouldProduceUniqueStates() throws Exception {
        String a = invokeGenerateState("myapp");
        String b = invokeGenerateState("myapp");
        assertNotEquals(a, b);
    }

    @Test
    void validateAndExtractState_shouldReturnTextBeforeFirstColon() throws Exception {
        String state = invokeGenerateState("myapp");
        Object extracted = invokePrivate("validateAndExtractState", state);
        assertEquals("myapp", extracted);
    }

    @Test
    void validateAndExtractState_shouldReturnNull_forInvalidBase64() throws Exception {
        Object extracted = invokePrivate("validateAndExtractState", "!!!not-base64!!!");
        assertNull(extracted);
    }

    @Test
    void validateAndExtractState_shouldReturnNull_whenNoColon() throws Exception {
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString("nocolon".getBytes());
        assertNull(invokePrivate("validateAndExtractState", state));
    }

    @Test
    void validateAndExtractState_shouldReturnDefault_whenRedirectUriWasEmpty() throws Exception {
        String state = invokeGenerateState("");
        Object extracted = invokePrivate("validateAndExtractState", state);
        assertEquals("default", extracted);
    }

    @Test
    void exchangeCodeForUserInfo_unsupportedProvider_shouldThrow() {
        Exception ex = assertThrows(Exception.class,
                () -> invokePrivate("exchangeCodeForUserInfo", "facebook", "code"));
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        assertTrue(cause instanceof BusinessException);
        assertTrue(cause.getMessage().contains("Unsupported OAuth provider"));
    }
}