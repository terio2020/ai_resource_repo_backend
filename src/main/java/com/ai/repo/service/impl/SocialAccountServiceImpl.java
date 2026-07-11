package com.ai.repo.service.impl;

import com.ai.repo.entity.SocialAccount;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SocialAccountMapper;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.SocialAccountService;
import com.ai.repo.util.UuidUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SocialAccountServiceImpl implements SocialAccountService {

    @Resource
    private SocialAccountMapper socialAccountMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TokenEncryptionService tokenEncryptionService;

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

    @Value("${oauth.apple.client-id:}")
    private String appleClientId;

    @Value("${oauth.wechat.client-id:}")
    private String wechatClientId;

    @Value("${oauth.state-secret:default-state-secret-change-in-production}")
    private String stateSecret;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String DEFAULT_STATE_SECRET = "default-state-secret-change-in-production";
    private static final int MIN_STATE_SECRET_BYTES = 32;

    @PostConstruct
    public void validateStateSecret() {
        if (stateSecret == null || stateSecret.isBlank()) {
            throw new IllegalStateException(
                "oauth.state-secret is not configured. Set the APP_OAUTH_STATE_SECRET environment variable.");
        }
        if (stateSecret.equals(DEFAULT_STATE_SECRET)) {
            throw new IllegalStateException(
                "oauth.state-secret is set to the public default value. Set APP_OAUTH_STATE_SECRET to a unique, "
                    + "secret value of at least " + MIN_STATE_SECRET_BYTES + " bytes.");
        }
        if (stateSecret.getBytes().length < MIN_STATE_SECRET_BYTES) {
            throw new IllegalStateException(
                "oauth.state-secret must be at least " + MIN_STATE_SECRET_BYTES
                    + " bytes long to prevent OAuth state forgery.");
        }
        log.info("OAuth state secret validated (length={} bytes).", stateSecret.getBytes().length);
    }

    @Override
    public SocialAccount findByUid(String uid) {
        return socialAccountMapper.selectByUid(uid);
    }

    @Override
    public SocialAccount findByProviderAndProviderUserId(String provider, String providerUserId) {
        return socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public List<SocialAccount> findByUserId(Long userId) {
        return socialAccountMapper.selectByUserId(userId);
    }

    @Override
    public SocialAccount findByUserIdAndProvider(Long userId, String provider) {
        return socialAccountMapper.selectByUserIdAndProvider(userId, provider);
    }

    @Override
    @Transactional
    public User linkSocialAccountToNewUser(String provider, String providerUserId, String email,
                                            String nickname, String avatar, String accessToken,
                                            String refreshToken, Long expiresIn) {
        // Check if social account already exists
        SocialAccount existing = socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
        if (existing != null) {
            throw new BusinessException(400, "Social account already linked to a user");
        }

        // Generate unique username from provider and providerUserId
        String baseUsername = provider + "_" + providerUserId;
        String username = generateUniqueUsername(baseUsername);
        if (username == null) {
            throw new BusinessException(500, "Failed to generate unique username");
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setPassword(""); // No password for social login users
        user.setEmail(email != null ? email : "");
        user.setNickname(nickname != null ? nickname : username);
        user.setAvatar(avatar != null ? avatar : "");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        
        userMapper.insert(user);
        log.info("Created new user via social login: {}, provider: {}", username, provider);

        // Create social account link
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUid(UuidUtil.generate());
        socialAccount.setUserId(user.getId());
        socialAccount.setProvider(provider);
        socialAccount.setProviderUserId(providerUserId);
        socialAccount.setAccessToken(tokenEncryptionService.encrypt(accessToken));
        socialAccount.setRefreshToken(tokenEncryptionService.encrypt(refreshToken));
        socialAccount.setEmail(email);
        socialAccount.setNickname(nickname);
        socialAccount.setAvatar(avatar);
        socialAccount.setTokenExpiresAt(expiresIn != null ? LocalDateTime.now().plusSeconds(expiresIn) : null);
        socialAccount.setCreatedAt(LocalDateTime.now());
        socialAccount.setUpdatedAt(LocalDateTime.now());
        
        socialAccountMapper.insert(socialAccount);
        log.info("Linked social account to user: {}, provider: {}", username, provider);

        return user;
    }

    @Override
    @Transactional
    public SocialAccount linkSocialAccountToExistingUser(Long userId, String provider,
                                                          String providerUserId, String email,
                                                          String nickname, String avatar,
                                                          String accessToken, String refreshToken,
                                                          Long expiresIn) {
        // Check if this social account is already linked to another user
        SocialAccount existing = socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
        if (existing != null && !existing.getUserId().equals(userId)) {
            throw new BusinessException(400, "This social account is already linked to another user");
        }

        // Check if user already has this provider linked
        SocialAccount userAccount = socialAccountMapper.selectByUserIdAndProvider(userId, provider);
        if (userAccount != null) {
            throw new BusinessException(400, "This provider is already linked to your account");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }

        // Create social account link
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUid(UuidUtil.generate());
        socialAccount.setUserId(userId);
        socialAccount.setProvider(provider);
        socialAccount.setProviderUserId(providerUserId);
        socialAccount.setAccessToken(tokenEncryptionService.encrypt(accessToken));
        socialAccount.setRefreshToken(tokenEncryptionService.encrypt(refreshToken));
        socialAccount.setEmail(email);
        socialAccount.setNickname(nickname);
        socialAccount.setAvatar(avatar);
        socialAccount.setTokenExpiresAt(expiresIn != null ? LocalDateTime.now().plusSeconds(expiresIn) : null);
        socialAccount.setCreatedAt(LocalDateTime.now());
        socialAccount.setUpdatedAt(LocalDateTime.now());
        
        socialAccountMapper.insert(socialAccount);
        log.info("Linked social account to existing user: {}, provider: {}", user.getUsername(), provider);

        return socialAccount;
    }

    @Override
    public User authenticateWithSocialAccount(String provider, String providerUserId) {
        SocialAccount socialAccount = socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
        if (socialAccount == null) {
            return null;
        }
        
        User user = userMapper.selectById(socialAccount.getUserId());
        if (user == null) {
            log.warn("Social account {}:{} linked to non-existent user", provider, providerUserId);
            return null;
        }
        
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "User account is not active");
        }
        
        return user;
    }

    @Override
    @Transactional
    public boolean unlinkSocialAccount(Long userId, String provider) {
        SocialAccount socialAccount = socialAccountMapper.selectByUserIdAndProvider(userId, provider);
        if (socialAccount == null) {
            throw new BusinessException(404, "Social account not found");
        }
        
        socialAccountMapper.deleteById(socialAccount.getId());
        log.info("Unlinked social account: userId={}, provider={}", userId, provider);
        return true;
    }

    @Override
    @Transactional
    public void updateTokens(Long socialAccountId, String accessToken, String refreshToken, Long expiresIn) {
        SocialAccount socialAccount = socialAccountMapper.selectById(socialAccountId);
        if (socialAccount == null) {
            throw new BusinessException(404, "Social account not found");
        }
        
        socialAccount.setAccessToken(tokenEncryptionService.encrypt(accessToken));
        socialAccount.setRefreshToken(tokenEncryptionService.encrypt(refreshToken));
        socialAccount.setTokenExpiresAt(expiresIn != null ? LocalDateTime.now().plusSeconds(expiresIn) : null);
        socialAccount.setUpdatedAt(LocalDateTime.now());
        
        socialAccountMapper.update(socialAccount);
    }

    @Override
    public boolean isProviderConfigured(String provider) {
        switch (provider.toLowerCase()) {
            case "google":
                return googleClientId != null && !googleClientId.isEmpty();
            case "github":
                return githubClientId != null && !githubClientId.isEmpty();
            case "apple":
                return appleClientId != null && !appleClientId.isEmpty();
            case "wechat":
                return wechatClientId != null && !wechatClientId.isEmpty();
            default:
                return false;
        }
    }

    private String generateUniqueUsername(String baseUsername) {
        // baseUsername like "google_123456789"
        String username = baseUsername.length() > 50 ? baseUsername.substring(0, 50) : baseUsername;
        
        // Check if exists, if so add random suffix
        if (userMapper.selectByUsername(username) == null) {
            return username;
        }
        
        // Try with random suffix
        for (int i = 0; i < 10; i++) {
            String candidate = username + "_" + UUID.randomUUID().toString().substring(0, 8);
            if (userMapper.selectByUsername(candidate) == null) {
                return candidate;
            }
        }
        
        return null; // Failed after 10 attempts
    }

    // ==================== OAuth protocol helpers ====================

    @Override
    public String buildAuthorizationUrl(String provider, String state) {
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

    @Override
    public String generateState(String redirectUri) {
        return generateState(redirectUri, null);
    }

    @Override
    public String generateState(String redirectUri, String sessionId) {
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString();
        String safeRedirectUri = (redirectUri != null && !redirectUri.isEmpty()) ? redirectUri : "default";
        String safeSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : "";
        String payload = safeRedirectUri + ":" + timestamp + ":" + random + ":" + safeSessionId;
        String signature = hmacSha256(payload);
        String rawState = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawState.getBytes());
    }

    @Override
    public OAuthState validateAndExtractState(String state) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(state));
            int lastColon = decoded.lastIndexOf(':');
            if (lastColon < 0) {
                return null;
            }
            String payload = decoded.substring(0, lastColon);
            String signature = decoded.substring(lastColon + 1);

            String expectedSignature = hmacSha256(payload);
            if (!expectedSignature.equals(signature)) {
                log.warn("State parameter HMAC signature mismatch");
                return null;
            }

            String[] parts = payload.split(":");
            if (parts.length >= 2) {
                String redirectUri = parts[0].isEmpty() ? null : parts[0];
                String sessionId = parts.length >= 4 ? parts[3] : "";
                return new OAuthState(redirectUri, sessionId.isEmpty() ? null : sessionId);
            }
            return null;
        } catch (Exception e) {
            log.warn("Invalid state parameter", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> exchangeCodeForUserInfo(String provider, String code) {
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

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);

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
            userInfo.put("refresh_token", null);
            userInfo.put("expires_in", null);

            return userInfo;
        } catch (Exception e) {
            log.error("Failed to exchange GitHub code", e);
            return null;
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(stateSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC-SHA256 not available", e);
            throw new RuntimeException("HMAC-SHA256 not available", e);
        }
    }
}