package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.LoginResponse;
import com.ai.repo.entity.SocialAccount;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.service.SocialAccountService;
import com.ai.repo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@RestController
@RequestMapping("/api/oauth")
@Tag(name = "OAuth API", description = "Social login via OAuth providers")
public class OAuthController {

    @Resource
    private SocialAccountService socialAccountService;

    @Resource
    private UserService userService;

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.google.client-secret:}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri:}")
    private String googleRedirectUri;

    @Value("${oauth.github.client-id:}")
    private String githubClientId;

    @Value("${oauth.github.client-secret:}")
    private String githubClientSecret;

    @Value("${oauth.github.redirect-uri:}")
    private String githubRedirectUri;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${oauth.state-secret:default-state-secret-change-in-production}")
    private String stateSecret;

    // ==================== Authorization Endpoints ====================

    @GetMapping("/{provider}")
    @Operation(summary = "Initiate OAuth login", description = "Redirect to provider's authorization page")
    public ResponseEntity<RedirectView> initiateOAuth(
            @Parameter(description = "OAuth provider (google, github)") @PathVariable String provider,
            @Parameter(description = "Redirect URL after successful login") @RequestParam(required = false) String redirect_uri) {

        if (!socialAccountService.isProviderConfigured(provider)) {
            throw new BusinessException(400, "OAuth provider not configured: " + provider);
        }

        String state = generateState(redirect_uri);
        String authUrl = buildAuthorizationUrl(provider, state);

        log.info("Initiating OAuth flow for provider: {}", provider);
        RedirectView redirectView = new RedirectView(authUrl);
        redirectView.setExposeModelAttributes(false);
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", authUrl).build();
    }

    @GetMapping("/{provider}/callback")
    @Operation(summary = "OAuth callback", description = "Handle OAuth provider callback")
    public Result<LoginResponse> handleOAuthCallback(
            @Parameter(description = "OAuth provider") @PathVariable String provider,
            @Parameter(description = "Authorization code") @RequestParam String code,
            @Parameter(description = "State token") @RequestParam String state) {

        // Validate state and extract redirect_uri
        String redirectUri = validateAndExtractState(state);
        if (redirectUri == null) {
            throw new BusinessException(400, "Invalid state parameter");
        }

        log.info("Handling OAuth callback for provider: {}", provider);

        // Exchange code for tokens and user info based on provider
        Map<String, Object> userInfo = exchangeCodeForUserInfo(provider, code);

        if (userInfo == null) {
            log.error("Failed to get user info from provider: {}", provider);
            throw new BusinessException(400, "Failed to authenticate with " + provider);
        }

        String providerUserId = (String) userInfo.get("id");
        String email = (String) userInfo.get("email");
        String nickname = (String) userInfo.get("name");
        String avatar = (String) userInfo.get("picture");
        String accessToken = (String) userInfo.get("access_token");
        String refreshToken = (String) userInfo.get("refresh_token");
        Long expiresIn = userInfo.get("expires_in") != null ? ((Number) userInfo.get("expires_in")).longValue() : null;

        // Authenticate or register user
        User user = socialAccountService.authenticateWithSocialAccount(provider, providerUserId);

        if (user == null) {
            // New user - create account
            user = socialAccountService.linkSocialAccountToNewUser(
                    provider, providerUserId, email, nickname, avatar,
                    accessToken, refreshToken, expiresIn
            );
            log.info("Created new user via {} social login: {}", provider, user.getUsername());
        } else {
            // Existing user - update tokens
            SocialAccount socialAccount = socialAccountService.findByUserIdAndProvider(user.getId(), provider);
            if (socialAccount != null) {
                socialAccountService.updateTokens(socialAccount.getId(), accessToken, refreshToken, expiresIn);
            }
            log.info("User logged in via {} social login: {}", provider, user.getUsername());
        }

        // Generate JWT tokens
        LoginResponse response = userService.generateTokens(user.getId());
        userService.updateLoginTime(user.getId());

        // If redirect_uri provided, redirect with token
        if (redirectUri != null && !redirectUri.isEmpty()) {
            // For frontend redirect with token
            // The frontend will receive the token and store it
        }

        return Result.success(response);
    }

    // ==================== Provider-specific token exchange ====================

    private Map<String, Object> exchangeCodeForUserInfo(String provider, String code) {
        switch (provider.toLowerCase()) {
            case "google":
                return exchangeGoogleCode(code);
            case "github":
                return exchangeGithubCode(code);
            default:
                throw new BusinessException(400, "Unsupported OAuth provider: " + provider);
        }
    }

    private Map<String, Object> exchangeGoogleCode(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        // Exchange code for access token
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);

        // Build form-encoded body as explicit string — Google only accepts application/x-www-form-urlencoded
        String redirectUri = googleRedirectUri != null && !googleRedirectUri.isEmpty()
                ? googleRedirectUri : baseUrl + "/api/oauth/google/callback";
        String formBody = "code=" + java.net.URLEncoder.encode(code, java.nio.charset.StandardCharsets.UTF_8)
                + "&client_id=" + java.net.URLEncoder.encode(googleClientId, java.nio.charset.StandardCharsets.UTF_8)
                + "&client_secret=" + java.net.URLEncoder.encode(googleClientSecret, java.nio.charset.StandardCharsets.UTF_8)
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        try {
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> requestEntity = new HttpEntity<>(formBody, tokenHeaders);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, requestEntity, Map.class);
            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                log.error("Failed to exchange Google code for token. Status: {}, Body: {}",
                    tokenResponse.getStatusCode(), tokenResponse.getBody());
                return null;
            }

            Map<String, Object> tokenData = tokenResponse.getBody();
            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Long expiresIn = tokenData.get("expires_in") != null ? ((Number) tokenData.get("expires_in")).longValue() : null;

            // Get user info
            Map<String, Object> userInfo = new HashMap<>();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> userResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, Map.class);
            if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                Map<String, Object> googleUser = userResponse.getBody();
                userInfo.put("id", googleUser.get("id"));
                userInfo.put("email", googleUser.get("email"));
                userInfo.put("name", googleUser.get("name"));
                userInfo.put("picture", googleUser.get("picture"));
                log.error("Google user info fetched successfully for id: {}", googleUser.get("id"));
            } else {
                log.error("Failed to fetch Google user info. Status: {}, Body: {}",
                    userResponse.getStatusCode(), userResponse.getBody());
            }

            userInfo.put("access_token", accessToken);
            userInfo.put("refresh_token", refreshToken);
            userInfo.put("expires_in", expiresIn);

            return userInfo;
        } catch (Exception e) {
            log.error("Failed to exchange Google code: {}", e.getMessage());
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException hce = (org.springframework.web.client.HttpClientErrorException) e;
                log.error("Google API error response body: {}", hce.getResponseBodyAsString());
            }
            return null;
        }
    }

    private Map<String, Object> exchangeGithubCode(String code) {
        String tokenUrl = "https://github.com/login/oauth/access_token";
        String userInfoUrl = "https://api.github.com/user";
        String emailsUrl = "https://api.github.com/user/emails";

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("code", code);
        tokenRequest.put("client_id", githubClientId);
        tokenRequest.put("client_secret", githubClientSecret);
        tokenRequest.put("redirect_uri", githubRedirectUri != null && !githubRedirectUri.isEmpty() 
                ? githubRedirectUri : baseUrl + "/api/oauth/github/callback");

        try {
            // Exchange code for access token (GitHub returns JSON-like text)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(tokenRequest, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, entity, Map.class);
            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                log.error("Failed to exchange GitHub code for token");
                return null;
            }

            Map<String, Object> tokenData = tokenResponse.getBody();
            String accessToken = (String) tokenData.get("access_token");
            if (accessToken == null) {
                log.error("No access token in GitHub response");
                return null;
            }

            // Get user info
            Map<String, Object> userInfo = new HashMap<>();
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userEntity, Map.class);
            if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                Map<String, Object> githubUser = userResponse.getBody();
                userInfo.put("id", String.valueOf(githubUser.get("id")));
                userInfo.put("name", githubUser.get("login"));
                userInfo.put("picture", githubUser.get("avatar_url"));

                // Try to get primary email
                try {
                    ResponseEntity<List> emailsResponse = restTemplate.exchange(emailsUrl, HttpMethod.GET, userEntity, List.class);
                    if (emailsResponse.getStatusCode() == HttpStatus.OK && emailsResponse.getBody() != null) {
                        List<?> emailsList = emailsResponse.getBody();
                        for (Object emailItem : emailsList) {
                            if (emailItem instanceof Map) {
                                Map<String, Object> emailObj = (Map<String, Object>) emailItem;
                                if (Boolean.TRUE.equals(emailObj.get("primary")) && Boolean.TRUE.equals(emailObj.get("verified"))) {
                                    userInfo.put("email", emailObj.get("email"));
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get GitHub emails", e);
                }
            }

            userInfo.put("access_token", accessToken);
            userInfo.put("refresh_token", null); // GitHub doesn't provide refresh token
            userInfo.put("expires_in", null);

            return userInfo;
        } catch (Exception e) {
            log.error("Failed to exchange GitHub code", e);
            return null;
        }
    }

    // ==================== Helper methods ====================

    private String buildAuthorizationUrl(String provider, String state) {
        switch (provider.toLowerCase()) {
            case "google":
                return "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=" + googleClientId +
                        "&redirect_uri=" + (googleRedirectUri != null && !googleRedirectUri.isEmpty() 
                                ? googleRedirectUri : baseUrl + "/api/oauth/google/callback") +
                        "&response_type=code" +
                        "&scope=openid%20email%20profile" +
                        "&state=" + state +
                        "&access_type=offline";
            case "github":
                return "https://github.com/login/oauth/authorize?" +
                        "client_id=" + githubClientId +
                        "&redirect_uri=" + (githubRedirectUri != null && !githubRedirectUri.isEmpty() 
                                ? githubRedirectUri : baseUrl + "/api/oauth/github/callback") +
                        "&scope=user:email" +
                        "&state=" + state;
            default:
                throw new BusinessException(400, "Unsupported OAuth provider: " + provider);
        }
    }

    private String generateState(String redirectUri) {
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString();
        String safeRedirectUri = (redirectUri != null && !redirectUri.isEmpty()) ? redirectUri : "default";
        String payload = safeRedirectUri + ":" + timestamp + ":" + random;
        String signature = hmacSha256(payload);
        String rawState = payload + ":" + signature;
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawState.getBytes());
    }

    private String validateAndExtractState(String state) {
        try {
            String decoded = new String(java.util.Base64.getUrlDecoder().decode(state));
            int lastColon = decoded.lastIndexOf(':');
            if (lastColon < 0) {
                return null;
            }
            String payload = decoded.substring(0, lastColon);
            String signature = decoded.substring(lastColon + 1);

            // Verify HMAC signature
            String expectedSignature = hmacSha256(payload);
            if (!expectedSignature.equals(signature)) {
                log.warn("State parameter HMAC signature mismatch");
                return null;
            }

            String[] parts = payload.split(":");
            if (parts.length >= 2) {
                return parts[0].isEmpty() ? null : parts[0];
            }
            return null;
        } catch (Exception e) {
            log.warn("Invalid state parameter", e);
            return null;
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(stateSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC-SHA256 not available", e);
            throw new RuntimeException("HMAC-SHA256 not available", e);
        }
    }
}