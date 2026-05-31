package com.ai.repo.service.impl;

import com.ai.repo.dto.PasswordResetConfirmRequest;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoderUtil passwordEncoderUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JavaMailSender javaMailSender;

    private PasswordResetServiceImpl passwordResetService;

    private static final String VALID_EMAIL = "test@example.com";
    private static final String NONEXISTENT_EMAIL = "nobody@nowhere.com";
    private static final String VALID_TOKEN = "valid-token-abc123";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final Long USER_ID = 1L;
    private static final String NEW_PASSWORD = "NewPwd123!";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedhash";

    @BeforeEach
    void setUp() throws Exception {
        passwordResetService = new PasswordResetServiceImpl();

        // Use reflection to inject mocked dependencies
        injectField("userMapper", userMapper);
        injectField("userService", userService);
        injectField("passwordEncoderUtil", passwordEncoderUtil);
        injectField("redisTemplate", redisTemplate);
        injectField("javaMailSender", javaMailSender);
        injectField("fromEmail", "noreply@logicoma.ai");
        injectField("frontendUrl", "http://localhost:3000");

        // Stub Redis opsForValue
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = PasswordResetServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(passwordResetService, value);
    }

    // ========================================================================
    // requestPasswordReset() tests
    // ========================================================================

    @Test
    void requestPasswordReset_shouldSendEmail_whenValidEmail() {
        // Given
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(VALID_EMAIL);
        user.setUsername("testuser");

        when(redisTemplate.hasKey("password_reset_rate:" + VALID_EMAIL)).thenReturn(false);
        when(userMapper.selectByEmail(VALID_EMAIL)).thenReturn(user);
        lenient().doNothing().when(valueOperations).set(anyString(), any(Object.class), anyLong(), any(TimeUnit.class));
        doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

        // When
        passwordResetService.requestPasswordReset(VALID_EMAIL);

        // Then
        verify(userMapper).selectByEmail(VALID_EMAIL);
        verify(valueOperations, atLeastOnce()).set(anyString(), any(Object.class), anyLong(), any(TimeUnit.class));
        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestPasswordReset_shouldNotThrow_whenEmailNotFound() {
        // Given
        when(redisTemplate.hasKey("password_reset_rate:" + NONEXISTENT_EMAIL)).thenReturn(false);
        when(userMapper.selectByEmail(NONEXISTENT_EMAIL)).thenReturn(null);

        // When / Then — should NOT throw despite no user found (account enumeration prevention)
        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(NONEXISTENT_EMAIL));
        verify(userMapper).selectByEmail(NONEXISTENT_EMAIL);
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void validateResetToken_shouldReturnTrue_whenTokenExists() {
        // Given
        String redisKey = "password_reset:" + VALID_TOKEN;
        when(redisTemplate.hasKey(redisKey)).thenReturn(true);

        // When
        boolean result = passwordResetService.validateResetToken(VALID_TOKEN);

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey(redisKey);
    }

    @Test
    void validateResetToken_shouldReturnFalse_whenTokenNotExists() {
        // Given
        String redisKey = "password_reset:" + INVALID_TOKEN;
        when(redisTemplate.hasKey(redisKey)).thenReturn(false);

        // When
        boolean result = passwordResetService.validateResetToken(INVALID_TOKEN);

        // Then
        assertFalse(result);
    }

    @Test
    void validateResetToken_shouldReturnFalse_whenTokenIsNull() {
        assertFalse(passwordResetService.validateResetToken(null));
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void validateResetToken_shouldReturnFalse_whenTokenIsEmpty() {
        assertFalse(passwordResetService.validateResetToken(""));
        verify(redisTemplate, never()).hasKey(anyString());
    }

    // ========================================================================
    // confirmPasswordReset() tests
    // ========================================================================

    @Test
    void confirmPasswordReset_shouldResetPassword_whenValidToken() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(VALID_TOKEN);
        request.setNewPassword(NEW_PASSWORD);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(VALID_EMAIL);
        user.setUsername("testuser");
        user.setPassword("old-hashed-password");

        String redisKey = "password_reset:" + VALID_TOKEN;

        when(valueOperations.get(redisKey)).thenReturn(USER_ID.toString());
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoderUtil.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userMapper.update(any())).thenReturn(1);
        when(redisTemplate.delete(redisKey)).thenReturn(true);
        doNothing().when(userService).clearTokens(USER_ID);

        // When
        passwordResetService.confirmPasswordReset(request);

        // Then
        verify(passwordEncoderUtil).encode(NEW_PASSWORD);
        verify(userMapper).update(argThat(u -> ENCODED_PASSWORD.equals(u.getPassword())));
        verify(redisTemplate).delete(redisKey);
        verify(userService).clearTokens(USER_ID);
        verify(javaMailSender, atLeastOnce()).send(any(SimpleMailMessage.class));
    }

    @Test
    void confirmPasswordReset_shouldThrow400_whenTokenNotFound() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(INVALID_TOKEN);
        request.setNewPassword(NEW_PASSWORD);

        String redisKey = "password_reset:" + INVALID_TOKEN;
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When / Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordResetService.confirmPasswordReset(request));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("Invalid") || ex.getMessage().contains("expired"));
        verify(userMapper, never()).selectById(any());
        verify(userService, never()).clearTokens(any());
    }

    @Test
    void confirmPasswordReset_shouldThrow400_whenTokenReused() {
        // Given — first call succeeds
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(VALID_TOKEN);
        request.setNewPassword(NEW_PASSWORD);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(VALID_EMAIL);
        user.setUsername("testuser");
        user.setPassword("old-hash");

        String redisKey = "password_reset:" + VALID_TOKEN;

        // First call: token exists
        when(valueOperations.get(redisKey)).thenReturn(USER_ID.toString());
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoderUtil.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(redisTemplate.delete(redisKey)).thenReturn(true);

        passwordResetService.confirmPasswordReset(request);

        // Second call: token already deleted
        when(valueOperations.get(redisKey)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordResetService.confirmPasswordReset(request));
        assertEquals(400, ex.getCode());
    }

    @Test
    void confirmPasswordReset_shouldThrow404_whenUserNotFound() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(VALID_TOKEN);
        request.setNewPassword(NEW_PASSWORD);

        String redisKey = "password_reset:" + VALID_TOKEN;

        when(valueOperations.get(redisKey)).thenReturn(USER_ID.toString());
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        // When / Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordResetService.confirmPasswordReset(request));
        assertEquals(404, ex.getCode());
        verify(redisTemplate, never()).delete(anyString());
        verify(userService, never()).clearTokens(any());
    }

    @Test
    void confirmPasswordReset_shouldThrow400_whenTokenParseFails() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(VALID_TOKEN);
        request.setNewPassword(NEW_PASSWORD);

        String redisKey = "password_reset:" + VALID_TOKEN;
        // Non-numeric user ID stored in Redis
        when(valueOperations.get(redisKey)).thenReturn("not-a-number");

        // When / Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordResetService.confirmPasswordReset(request));
        assertEquals(400, ex.getCode());
    }

    // ========================================================================
    // invalidateUserSessions() tests
    // ========================================================================

    @Test
    void invalidateUserSessions_shouldClearTokens() {
        // Given
        doNothing().when(userService).clearTokens(USER_ID);

        // When
        passwordResetService.invalidateUserSessions(USER_ID);

        // Then
        verify(userService).clearTokens(USER_ID);
    }
}
