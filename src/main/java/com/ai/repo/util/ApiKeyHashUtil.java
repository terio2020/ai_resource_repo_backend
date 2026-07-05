package com.ai.repo.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * HMAC-SHA256 keyed hash for API keys.
 *
 * <p>Uses the same {@code token.encryption.secret} as {@link TokenEncryptionService}
 * but derives a keyed-hash (HMAC) instead of an AES key. The output is a
 * Base64url-encoded 32-byte digest stored in {@code agents.api_key_hash}.
 *
 * <p>Because the hash is deterministic, lookups are exact-match queries against
 * the hash column — the plaintext key is never used in a WHERE clause.
 */
@Slf4j
@Component
public class ApiKeyHashUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${token.encryption.secret:}")
    private String secret;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            log.warn("token.encryption.secret is not set — ApiKeyHashUtil will fail at runtime");
        }
    }

    /**
     * Compute an HMAC-SHA256 digest of {@code apiKey} and return it as a
     * Base64url-encoded string.
     */
    public String hash(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey must not be null or empty");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 not available", e);
        }
    }
}
