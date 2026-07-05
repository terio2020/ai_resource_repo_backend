package com.ai.repo.service.impl;

import com.ai.repo.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenEncryptionServiceTest {

    private TokenEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new TokenEncryptionService();
        ReflectionTestUtils.setField(service, "secret", "this-is-a-32-byte-test-secret-1234567890!!");
        service.validateSecret();
    }

    @Test
    void roundtrip_shouldReturnOriginal() {
        String plain = "ya29.a0AfH6SMB...very-long-google-token";
        String encrypted = service.encrypt(plain);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);
        assertTrue(encrypted.length() > 20);

        String decrypted = service.decrypt(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    void encrypt_shouldReturnNull_whenInputNull() {
        assertNull(service.encrypt(null));
    }

    @Test
    void encrypt_shouldReturnEmpty_whenInputEmpty() {
        assertEquals("", service.encrypt(""));
    }

    @Test
    void decrypt_shouldReturnNull_whenInputNull() {
        assertNull(service.decrypt(null));
    }

    @Test
    void decrypt_shouldReturnEmpty_whenInputEmpty() {
        assertEquals("", service.decrypt(""));
    }

    @Test
    void decrypt_shouldThrow_whenTampered() {
        String encrypted = service.encrypt("my-token");
        // Flip a byte in the Base64 payload
        char[] chars = encrypted.toCharArray();
        chars[chars.length / 2] = (char) (chars[chars.length / 2] ^ 0x01);
        String tampered = new String(chars);

        assertThrows(BusinessException.class, () -> service.decrypt(tampered));
    }

    @Test
    void decrypt_shouldThrow_whenWrongKey() {
        String encrypted = service.encrypt("my-token");

        TokenEncryptionService other = new TokenEncryptionService();
        ReflectionTestUtils.setField(other, "secret", "a-completely-different-32-byte-secret-string!!!");
        other.validateSecret();

        assertThrows(BusinessException.class, () -> other.decrypt(encrypted));
    }

    @Test
    void decrypt_shouldThrow_whenInvalidBase64() {
        assertThrows(BusinessException.class, () -> service.decrypt("not-base64!!!"));
    }

    @Test
    void decrypt_shouldThrow_whenTooShort() {
        assertThrows(BusinessException.class, () -> service.decrypt("c2hvcnQ="));
    }

    @Test
    void validateSecret_shouldThrow_whenNull() {
        TokenEncryptionService svc = new TokenEncryptionService();
        ReflectionTestUtils.setField(svc, "secret", null);
        assertThrows(IllegalStateException.class, svc::validateSecret);
    }

    @Test
    void validateSecret_shouldThrow_whenBlank() {
        TokenEncryptionService svc = new TokenEncryptionService();
        ReflectionTestUtils.setField(svc, "secret", "   ");
        assertThrows(IllegalStateException.class, svc::validateSecret);
    }

    @Test
    void validateSecret_shouldThrow_whenDefault() {
        TokenEncryptionService svc = new TokenEncryptionService();
        ReflectionTestUtils.setField(svc, "secret", "change-me-token-encryption-secret-32-bytes-min!!");
        assertThrows(IllegalStateException.class, svc::validateSecret);
    }

    @Test
    void validateSecret_shouldThrow_whenTooShort() {
        TokenEncryptionService svc = new TokenEncryptionService();
        ReflectionTestUtils.setField(svc, "secret", "short");
        assertThrows(IllegalStateException.class, svc::validateSecret);
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertexts_eachCall() {
        String plain = "same-token";
        String e1 = service.encrypt(plain);
        String e2 = service.encrypt(plain);
        assertNotEquals(e1, e2);
    }
}
