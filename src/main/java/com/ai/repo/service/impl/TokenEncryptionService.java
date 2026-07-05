package com.ai.repo.service.impl;

import com.ai.repo.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for OAuth tokens stored in the database.
 *
 * <p>Uses a user-supplied secret (from {@code token.encryption.secret}) to derive a
 * 256-bit AES key via SHA-256. Each encryption generates a fresh 12-byte random IV
 * that is prepended to the ciphertext; the combined blob is Base64-encoded for
 * storage in a VARCHAR column.
 *
 * <p>GCM provides authenticated encryption — any tampering with the ciphertext or IV
 * is detected on decryption. A {@code @PostConstruct} guard fails fast at startup if
 * the secret is missing, still set to the public default, or shorter than 32 bytes.
 */
@Slf4j
@Service
public class TokenEncryptionService {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String DEFAULT_SECRET = "change-me-token-encryption-secret-32-bytes-min!!";
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${token.encryption.secret:}")
    private String secret;

    private SecretKey aesKey;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "token.encryption.secret is not configured. Set the TOKEN_ENCRYPTION_SECRET environment variable.");
        }
        if (secret.equals(DEFAULT_SECRET)) {
            throw new IllegalStateException(
                "token.encryption.secret is set to the public default value. Set TOKEN_ENCRYPTION_SECRET to a unique, "
                    + "secret value of at least " + MIN_SECRET_BYTES + " bytes.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "token.encryption.secret must be at least " + MIN_SECRET_BYTES
                    + " bytes long to provide adequate AES-256 key entropy.");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = md.digest(secret.getBytes(StandardCharsets.UTF_8));
            aesKey = new SecretKeySpec(keyBytes, "AES");
            log.info("Token encryption key derived (SHA-256 → AES-256).");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive AES key from token.encryption.secret", e);
        }
    }

    /**
     * Encrypt {@code plaintext} with AES-256-GCM.
     *
     * @return Base64-encoded {@code IV ‖ ciphertext}, or the input unchanged when it is
     *         {@code null} or empty (so callers don't need null guards).
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to encrypt token: " + e.getMessage());
        }
    }

    /**
     * Decrypt a value previously produced by {@link #encrypt(String)}.
     *
     * @return the original plaintext, or the input unchanged when it is {@code null} or empty.
     * @throws BusinessException if the ciphertext is invalid, tampered with, or was encrypted
     *                           with a different key.
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length < GCM_IV_LENGTH + 1) {
                throw new BusinessException(500, "Invalid encrypted token length");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to decrypt token: " + e.getMessage());
        }
    }
}
