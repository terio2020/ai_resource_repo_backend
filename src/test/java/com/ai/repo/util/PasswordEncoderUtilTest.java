package com.ai.repo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderUtilTest {

    private final PasswordEncoderUtil encoder = new PasswordEncoderUtil();

    @Test
    void encode_shouldReturnNonNullForValidPassword() {
        String encoded = encoder.encode("password123");
        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
    }

    @Test
    void encode_shouldReturnNullForNullInput() {
        assertNull(encoder.encode(null));
    }

    @Test
    void encode_shouldReturnNullForEmptyString() {
        assertNull(encoder.encode(""));
    }

    @Test
    void matches_shouldReturnTrueForMatchingPassword() {
        String encoded = encoder.encode("password123");
        assertTrue(encoder.matches("password123", encoded));
    }

    @Test
    void matches_shouldReturnFalseForWrongPassword() {
        String encoded = encoder.encode("password123");
        assertFalse(encoder.matches("wrongpassword", encoded));
    }

    @Test
    void matches_shouldReturnFalseWhenRawPasswordIsNull() {
        assertFalse(encoder.matches(null, "$2a$10$hash"));
    }

    @Test
    void matches_shouldReturnFalseWhenEncodedPasswordIsNull() {
        assertFalse(encoder.matches("password", null));
    }

    @Test
    void needsEncoding_shouldReturnTrueForNonEncodedPassword() {
        assertTrue(encoder.needsEncoding("plain-password"));
    }

    @Test
    void needsEncoding_shouldReturnFalseForBCryptPassword() {
        assertFalse(encoder.needsEncoding("$2a$10$hashvaluehere12345678901234567890"));
    }

    @Test
    void needsEncoding_shouldReturnFalseForNull() {
        assertFalse(encoder.needsEncoding(null));
    }

    @Test
    void needsEncoding_shouldReturnFalseForEmpty() {
        assertFalse(encoder.needsEncoding(""));
    }
}
