package com.ai.repo.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtProviderTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private JwtProvider jwtProvider;

    private static final String TEST_SECRET = "test-secret-key-for-testing-purposes-only-12345678901234567890";

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        jwtProvider = new JwtProvider(redisTemplate);

        java.lang.reflect.Field secretField = JwtProvider.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtProvider, TEST_SECRET);

        java.lang.reflect.Field accessExpField = JwtProvider.class.getDeclaredField("accessTokenExpiration");
        accessExpField.setAccessible(true);
        accessExpField.set(jwtProvider, 3600000L);

        java.lang.reflect.Field refreshExpField = JwtProvider.class.getDeclaredField("refreshTokenExpiration");
        refreshExpField.setAccessible(true);
        refreshExpField.set(jwtProvider, 604800000L);
    }

    @Test
    void generateAccessToken_shouldReturnNonNullToken() {
        String token = jwtProvider.generateAccessToken(1L, "testuser");
        assertNotNull(token);
        assertTrue(token.length() > 0);
        verify(valueOperations).set(eq("token:access:1"), eq(token), eq(3600000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void generateRefreshToken_shouldReturnNonNullToken() {
        String token = jwtProvider.generateRefreshToken(1L);
        assertNotNull(token);
        assertTrue(token.length() > 0);
        verify(valueOperations).set(eq("token:refresh:1"), eq(token), eq(604800000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void validateAccessToken_shouldReturnUserId_whenValid() {
        String token = jwtProvider.generateAccessToken(1L, "testuser");

        when(valueOperations.get("token:access:1")).thenReturn(token);

        Long userId = jwtProvider.validateAccessToken(token);
        assertEquals(1L, userId);
    }

    @Test
    void validateAccessToken_shouldReturnNull_whenTokenNotInRedis() {
        String token = jwtProvider.generateAccessToken(1L, "testuser");

        when(valueOperations.get("token:access:1")).thenReturn(null);

        Long userId = jwtProvider.validateAccessToken(token);
        assertNull(userId);
    }

    @Test
    void validateAccessToken_shouldReturnNull_whenTokenMismatch() {
        String token = jwtProvider.generateAccessToken(1L, "testuser");

        when(valueOperations.get("token:access:1")).thenReturn("different-token");

        Long userId = jwtProvider.validateAccessToken(token);
        assertNull(userId);
    }

    @Test
    void validateAccessToken_shouldReturnNull_whenExpired() throws Exception {
        java.lang.reflect.Field accessExpField = JwtProvider.class.getDeclaredField("accessTokenExpiration");
        accessExpField.setAccessible(true);
        accessExpField.set(jwtProvider, -1000L);

        String token = jwtProvider.generateAccessToken(1L, "testuser");

        Long userId = jwtProvider.validateAccessToken(token);
        assertNull(userId);
    }

    @Test
    void validateAccessToken_shouldReturnNull_whenInvalidToken() {
        Long userId = jwtProvider.validateAccessToken("invalid.jwt.token");
        assertNull(userId);
    }

    @Test
    void validateRefreshToken_shouldReturnUserId_whenValid() {
        String token = jwtProvider.generateRefreshToken(1L);

        when(valueOperations.get("token:refresh:1")).thenReturn(token);

        Long userId = jwtProvider.validateRefreshToken(token);
        assertEquals(1L, userId);
    }

    @Test
    void clearTokens_shouldDeleteThreeKeys() {
        jwtProvider.clearTokens(1L);

        verify(redisTemplate).delete("token:access:1");
        verify(redisTemplate).delete("token:refresh:1");
        verify(redisTemplate).delete("token:expires:1");
    }

    @Test
    void isTokenExpired_shouldReturnTrue_whenNoExpiresKey() {
        when(valueOperations.get("token:expires:1")).thenReturn(null);

        assertTrue(jwtProvider.isTokenExpired(1L));
    }

    @Test
    void isTokenExpired_shouldReturnFalse_whenNotExpired() {
        String futureExpires = String.valueOf(System.currentTimeMillis() + 3600000);
        when(valueOperations.get("token:expires:1")).thenReturn(futureExpires);

        assertFalse(jwtProvider.isTokenExpired(1L));
    }

    @Test
    void isTokenExpired_shouldReturnTrue_whenExpired() {
        String pastExpires = String.valueOf(System.currentTimeMillis() - 1000);
        when(valueOperations.get("token:expires:1")).thenReturn(pastExpires);

        assertTrue(jwtProvider.isTokenExpired(1L));
    }

    @Test
    void isTokenExpired_shouldReturnTrue_whenInvalidFormat() {
        when(valueOperations.get("token:expires:1")).thenReturn("not-a-number");

        assertTrue(jwtProvider.isTokenExpired(1L));
    }
}
