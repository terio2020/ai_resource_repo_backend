package com.ai.repo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TempTokenService {

    private static final long TOKEN_EXPIRY_MINUTES = 5;

    private final Map<String, TokenWithExpiry> tokenStore = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public TempTokenService() {
        // Schedule cleanup task to run every minute
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Store a temporary token with expiration
     */
    public String storeToken(String sessionId, String accessToken) {
        long expiryTime = System.currentTimeMillis() + TOKEN_EXPIRY_MINUTES * 60 * 1000;
        tokenStore.put(sessionId, new TokenWithExpiry(accessToken, expiryTime));
        log.info("Stored temporary token for sessionId: {}", sessionId);
        return sessionId;
    }

    /**
     * Get and remove a temporary token (one-time use)
     */
    public String getAndRemoveToken(String sessionId) {
        TokenWithExpiry tokenWithExpiry = tokenStore.remove(sessionId);
        if (tokenWithExpiry == null) {
            log.warn("Token not found for sessionId: {}", sessionId);
            return null;
        }

        // Check if expired
        if (System.currentTimeMillis() > tokenWithExpiry.expiryTime) {
            log.warn("Token expired for sessionId: {}", sessionId);
            return null;
        }

        log.info("Retrieved and removed temporary token for sessionId: {}", sessionId);
        return tokenWithExpiry.accessToken;
    }

    /**
     * Check if a token exists for the sessionId
     */
    public boolean hasToken(String sessionId) {
        TokenWithExpiry tokenWithExpiry = tokenStore.get(sessionId);
        if (tokenWithExpiry == null) {
            return false;
        }

        // Check if expired
        if (System.currentTimeMillis() > tokenWithExpiry.expiryTime) {
            tokenStore.remove(sessionId);
            return false;
        }

        return true;
    }

    /**
     * Cleanup expired tokens
     */
    private void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int removedCount = 0;
        String tokenStoreKey;

        for (Map.Entry<String, TokenWithExpiry> entry : tokenStore.entrySet()) {
            if (now > entry.getValue().expiryTime) {
                tokenStoreKey = entry.getKey();
                if (tokenStore.remove(tokenStoreKey) != null) {
                    removedCount++;
                }
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired temporary tokens", removedCount);
        }
    }

    /**
     * Inner class to hold token with expiration time
     */
    private static class TokenWithExpiry {
        String accessToken;
        long expiryTime;

        TokenWithExpiry(String accessToken, long expiryTime) {
            this.accessToken = accessToken;
            this.expiryTime = expiryTime;
        }
    }
}
