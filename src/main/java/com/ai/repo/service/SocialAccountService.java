package com.ai.repo.service;

import com.ai.repo.entity.SocialAccount;
import com.ai.repo.entity.User;

import java.util.List;
import java.util.Map;

public interface SocialAccountService {

    /**
     * Find social account by provider and provider user ID
     */
    SocialAccount findByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * Find all social accounts for a user
     */
    List<SocialAccount> findByUserId(Long userId);

    /**
     * Find social account by user ID and provider
     */
    SocialAccount findByUserIdAndProvider(Long userId, String provider);

    /**
     * Link social account to user (new user registration via social login)
     */
    User linkSocialAccountToNewUser(String provider, String providerUserId, String email, 
                                     String nickname, String avatar, String accessToken,
                                     String refreshToken, Long expiresIn);

    /**
     * Link social account to existing user
     */
    SocialAccount linkSocialAccountToExistingUser(Long userId, String provider, 
                                                  String providerUserId, String email,
                                                  String nickname, String avatar,
                                                  String accessToken, String refreshToken,
                                                  Long expiresIn);

    /**
     * Authenticate via social account - returns user if social account exists
     */
    User authenticateWithSocialAccount(String provider, String providerUserId);

    /**
     * Unlink social account from user
     */
    boolean unlinkSocialAccount(Long userId, String provider);

    /**
     * Update social account tokens
     */
    void updateTokens(Long socialAccountId, String accessToken, String refreshToken, Long expiresIn);

    /**
     * Check if provider is configured
     */
    boolean isProviderConfigured(String provider);

    /**
     * Build OAuth authorization URL for the given provider
     */
    String buildAuthorizationUrl(String provider, String state);

    /**
     * Generate OAuth state parameter with HMAC signature
     */
    String generateState(String redirectUri);

    /**
     * Generate OAuth state parameter with HMAC signature, with optional sessionId for agent binding
     */
    String generateState(String redirectUri, String sessionId);

    /**
     * Result of validating and parsing an OAuth state parameter
     */
    class OAuthState {
        private final String redirectUri;
        private final String sessionId;

        public OAuthState(String redirectUri, String sessionId) {
            this.redirectUri = redirectUri;
            this.sessionId = sessionId;
        }

        public String getRedirectUri() { return redirectUri; }
        public String getSessionId() { return sessionId; }
    }

    /**
     * Validate and extract redirect URI and sessionId from state parameter
     */
    OAuthState validateAndExtractState(String state);

    /**
     * Exchange authorization code for user info from the provider
     */
    Map<String, Object> exchangeCodeForUserInfo(String provider, String code);
}