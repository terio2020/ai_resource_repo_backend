package com.ai.repo.service.impl;

import com.ai.repo.dto.LoginResponse;
import com.ai.repo.dto.TokenRefreshResponse;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.jwt.JwtProvider;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.util.PasswordEncoderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock(lenient = true)
    private RedisTemplate<String, Object> redisTemplate;

    @Mock(lenient = true)
    private ValueOperations<String, Object> valueOperations;

    private UserServiceImpl userService;

    private PasswordEncoderUtil passwordEncoderUtil;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() throws Exception {
        passwordEncoderUtil = Mockito.spy(new PasswordEncoderUtil());
        
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        jwtProvider = Mockito.spy(new JwtProvider(redisTemplate));
        
        java.lang.reflect.Field secretField = JwtProvider.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtProvider, "test-secret-key-for-testing-purposes-only-12345678901234567890");
        
        userService = new UserServiceImpl();
        
        java.lang.reflect.Field fieldMapper = UserServiceImpl.class.getDeclaredField("userMapper");
        fieldMapper.setAccessible(true);
        fieldMapper.set(userService, userMapper);
        
        java.lang.reflect.Field fieldPassword = UserServiceImpl.class.getDeclaredField("passwordEncoderUtil");
        fieldPassword.setAccessible(true);
        fieldPassword.set(userService, passwordEncoderUtil);
        
        java.lang.reflect.Field fieldJwt = UserServiceImpl.class.getDeclaredField("jwtProvider");
        fieldJwt.setAccessible(true);
        fieldJwt.set(userService, jwtProvider);
    }

    // ===== create() tests =====

    @Test
    void create_shouldReturnUser_whenValidData() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(userMapper.selectByEmail("test@example.com")).thenReturn(null);
        when(passwordEncoderUtil.needsEncoding("password123")).thenReturn(false);
        when(userMapper.insert(any())).thenReturn(1);

        // When
        User result = userService.create(user);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void create_shouldThrowException_whenDuplicateUsername() {
        // Given
        User user = new User();
        user.setUsername("existinguser");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        when(userMapper.selectByUsername("existinguser")).thenReturn(new User());
        when(passwordEncoderUtil.needsEncoding(any())).thenReturn(false);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.create(user);
        });
        assertTrue(exception.getMessage().contains("Username already exists"));
    }

    @Test
    void create_shouldThrowException_whenDuplicateEmail() {
        // Given
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("existing@example.com");
        user.setPassword("password123");

        when(userMapper.selectByUsername("newuser")).thenReturn(null);
        when(userMapper.selectByEmail("existing@example.com")).thenReturn(new User());
        when(passwordEncoderUtil.needsEncoding(any())).thenReturn(false);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.create(user);
        });
        assertTrue(exception.getMessage().contains("Email already exists"));
    }

    @Test
    void create_shouldEncodePassword_whenNeedsEncoding() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("plainpassword");

        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(userMapper.selectByEmail("test@example.com")).thenReturn(null);
        when(passwordEncoderUtil.needsEncoding("plainpassword")).thenReturn(true);
        when(passwordEncoderUtil.encode("plainpassword")).thenReturn("encodedpassword");
        when(userMapper.insert(any())).thenReturn(1);

        // When
        User result = userService.create(user);

        // Then
        assertNotNull(result);
        assertEquals("encodedpassword", result.getPassword());
    }

    // ===== update() tests (merge-based partial update) =====

    @Test
    void update_shouldMergeNickname_whenProvided() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setNickname("new-nickname");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setNickname("old-nickname");
        existingUser.setEmail("old@example.com");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("new-nickname", result.getNickname());
        assertEquals("old@example.com", result.getEmail());
    }

    @Test
    void update_shouldThrowException_whenUserNotFound() {
        // Given
        User user = new User();
        user.setId(999L);

        when(userMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.update(user);
        });
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void update_shouldEncodePassword_whenNeedsEncoding() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setPassword("newplainpassword");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("oldencodedpassword");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(passwordEncoderUtil.needsEncoding("newplainpassword")).thenReturn(true);
        when(passwordEncoderUtil.encode("newplainpassword")).thenReturn("encodedpassword");
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("encodedpassword", result.getPassword());
    }

    @Test
    void update_shouldNotOverwritePassword_whenNull() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setNickname("new-nickname");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("existingencodedpassword");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("existingencodedpassword", result.getPassword());
    }

    @Test
    void update_shouldNotOverwritePassword_whenEmpty() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setPassword("");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("existingencodedpassword");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("existingencodedpassword", result.getPassword());
    }

    @Test
    void update_shouldMergeAvatar_whenProvided() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setAvatar("new-avatar.png");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setAvatar("old-avatar.png");
        existingUser.setNickname("unchanged");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("new-avatar.png", result.getAvatar());
        assertEquals("unchanged", result.getNickname());
    }

    @Test
    void update_shouldMergeXFields_whenProvided() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setXHandle("new-handle");
        user.setXName("new-x-name");
        user.setXAvatar("new-x-avatar.png");

        User existingUser = new User();
        existingUser.setId(1L);

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("new-handle", result.getXHandle());
        assertEquals("new-x-name", result.getXName());
        assertEquals("new-x-avatar.png", result.getXAvatar());
    }

    @Test
    void update_shouldThrowException_whenEmailTakenByAnotherUser() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("taken@example.com");

        User existingUser = new User();
        existingUser.setId(1L);

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("taken@example.com");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.selectByEmail("taken@example.com")).thenReturn(anotherUser);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.update(user);
        });
        assertTrue(exception.getMessage().contains("Email already exists"));
    }

    @Test
    void update_shouldAllowEmailUpdate_whenNotTaken() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("new@example.com");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.selectByEmail("new@example.com")).thenReturn(null);
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void update_shouldAllowEmailUpdate_whenSameAsOwnEmail() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("existing@example.com");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("existing@example.com");

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.selectByEmail("existing@example.com")).thenReturn(existingUser);
        when(userMapper.update(any())).thenReturn(1);

        // When
        User result = userService.update(user);

        // Then
        assertEquals("existing@example.com", result.getEmail());
    }

    // ===== delete() tests =====

    @Test
    void delete_shouldReturnTrue_whenUserExists() {
        // Given
        Long userId = 1L;
        when(userMapper.selectById(1L)).thenReturn(new User());
        when(userMapper.deleteById(1L)).thenReturn(1);

        // When
        boolean result = userService.delete(userId);

        // Then
        assertTrue(result);
    }

    @Test
    void delete_shouldThrowException_whenUserNotFound() {
        // Given
        Long userId = 999L;
        when(userMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.delete(userId);
        });
        assertTrue(exception.getMessage().contains("User not found"));
    }

    // ===== findById() tests =====

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(userMapper.selectById(1L)).thenReturn(user);

        // When
        User result = userService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldReturnNull_whenUserNotFound() {
        // Given
        when(userMapper.selectById(999L)).thenReturn(null);

        // When
        User result = userService.findById(999L);

        // Then
        assertNull(result);
    }

    // ===== findByUsername() tests =====

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        // Given
        User user = new User();
        user.setUsername("testuser");

        when(userMapper.selectByUsername("testuser")).thenReturn(user);

        // When
        User result = userService.findByUsername("testuser");

        // Then
        assertNotNull(result);
    }

    @Test
    void findByUsername_shouldReturnNull_whenUserNotFound() {
        // Given
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        // When
        User result = userService.findByUsername("nonexistent");

        // Then
        assertNull(result);
    }

    // ===== findByEmail() tests =====

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");

        when(userMapper.selectByEmail("test@example.com")).thenReturn(user);

        // When
        User result = userService.findByEmail("test@example.com");

        // Then
        assertNotNull(result);
    }

    @Test
    void findByEmail_shouldReturnNull_whenUserNotFound() {
        // Given
        when(userMapper.selectByEmail("nonexistent@example.com")).thenReturn(null);

        // When
        User result = userService.findByEmail("nonexistent@example.com");

        // Then
        assertNull(result);
    }

    // ===== findAll() tests =====

    @Test
    void findAll_shouldReturnAllUsers() {
        // Given
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);

        when(userMapper.selectAll()).thenReturn(Arrays.asList(user1, user2));

        // When
        var result = userService.findAll();

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoUsers() {
        // Given
        when(userMapper.selectAll()).thenReturn(Collections.emptyList());

        // When
        var result = userService.findAll();

        // Then
        assertTrue(result.isEmpty());
    }

    // ===== findByStatus() tests =====

    @Test
    void findByStatus_shouldReturnUsersByStatus() {
        // Given
        User user = new User();
        user.setStatus("active");

        when(userMapper.selectByStatus("active")).thenReturn(Arrays.asList(user));

        // When
        var result = userService.findByStatus("active");

        // Then
        assertEquals(1, result.size());
    }

    // ===== findByRole() tests =====

    @Test
    void findByRole_shouldReturnUsersByRole() {
        // Given
        User user = new User();
        user.setRole("admin");

        when(userMapper.selectByRole("admin")).thenReturn(Arrays.asList(user));

        // When
        var result = userService.findByRole("admin");

        // Then
        assertEquals(1, result.size());
    }

    // ===== verifyPassword() tests =====

    @Test
    void verifyPassword_shouldReturnTrue_whenPasswordCorrect() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedpassword");

        when(userMapper.selectByUsername("testuser")).thenReturn(user);
        when(passwordEncoderUtil.matches("rawpassword", "encodedpassword")).thenReturn(true);

        // When
        boolean result = userService.verifyPassword("testuser", "rawpassword");

        // Then
        assertTrue(result);
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenPasswordIncorrect() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedpassword");

        when(userMapper.selectByUsername("testuser")).thenReturn(user);
        when(passwordEncoderUtil.matches("wrongpassword", "encodedpassword")).thenReturn(false);

        // When
        boolean result = userService.verifyPassword("testuser", "wrongpassword");

        // Then
        assertFalse(result);
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenUserNotFound() {
        // Given
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        // When
        boolean result = userService.verifyPassword("nonexistent", "anypassword");

        // Then
        assertFalse(result);
    }

    // ===== verifyPasswordByEmail() tests =====

    @Test
    void verifyPasswordByEmail_shouldReturnTrue_whenPasswordCorrect() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("encodedpassword");

        when(userMapper.selectByEmail("test@example.com")).thenReturn(user);
        when(passwordEncoderUtil.matches("rawpassword", "encodedpassword")).thenReturn(true);

        // When
        boolean result = userService.verifyPasswordByEmail("test@example.com", "rawpassword");

        // Then
        assertTrue(result);
    }

    @Test
    void verifyPasswordByEmail_shouldReturnFalse_whenPasswordIncorrect() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("encodedpassword");

        when(userMapper.selectByEmail("test@example.com")).thenReturn(user);
        when(passwordEncoderUtil.matches("wrongpassword", "encodedpassword")).thenReturn(false);

        // When
        boolean result = userService.verifyPasswordByEmail("test@example.com", "wrongpassword");

        // Then
        assertFalse(result);
    }

    @Test
    void verifyPasswordByEmail_shouldReturnFalse_whenUserNotFound() {
        // Given
        when(userMapper.selectByEmail("nonexistent@example.com")).thenReturn(null);

        // When
        boolean result = userService.verifyPasswordByEmail("nonexistent@example.com", "anypassword");

        // Then
        assertFalse(result);
    }

    // ===== generateTokens() tests =====

    @Test
    void generateTokens_shouldReturnLoginResponse_whenUserExists() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setNickname("Test Nickname");
        user.setAvatar("avatar.png");
        user.setRole("user");
        user.setStatus("active");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(jwtProvider.generateAccessToken(1L, "testuser")).thenReturn("accessToken");
        when(jwtProvider.generateRefreshToken(1L)).thenReturn("refreshToken");
        when(userMapper.update(any())).thenReturn(1);

        // When
        LoginResponse result = userService.generateTokens(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("accessToken", result.getAccessToken());
    }

    @Test
    void generateTokens_shouldThrowException_whenUserNotFound() {
        // Given
        when(userMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.generateTokens(999L);
        });
        assertTrue(exception.getMessage().contains("User not found"));
    }

    // ===== refreshToken() tests =====

    @Test
    void refreshToken_shouldReturnTokenRefreshResponse_whenValidToken() {
        // Given
        String refreshToken = "validRefreshToken";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(jwtProvider.validateRefreshToken(refreshToken)).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(jwtProvider.generateAccessToken(1L, "testuser")).thenReturn("newAccessToken");
        when(userMapper.update(any())).thenReturn(1);

        // When
        TokenRefreshResponse result = userService.refreshToken(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
    }

    @Test
    void refreshToken_shouldThrowException_whenInvalidToken() {
        // Given
        String invalidToken = "invalidRefreshToken";
        when(jwtProvider.validateRefreshToken(invalidToken)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.refreshToken(invalidToken);
        });
        assertTrue(exception.getMessage().contains("Invalid refresh token"));
    }

    @Test
    void refreshToken_shouldThrowException_whenUserNotFound() {
        // Given
        String refreshToken = "validRefreshToken";
        when(jwtProvider.validateRefreshToken(refreshToken)).thenReturn(999L);
        when(userMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.refreshToken(refreshToken);
        });
        assertTrue(exception.getMessage().contains("User not found"));
    }

    // ===== clearTokens() tests =====

    @Test
    void clearTokens_shouldClearTokensAndUpdateUser() {
        // Given
        Long userId = 1L;
        User user = new User();
        user.setId(1L);

        doNothing().when(jwtProvider).clearTokens(userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        when(userMapper.update(any())).thenReturn(1);

        // When
        userService.clearTokens(userId);

        // Then
        verify(jwtProvider).clearTokens(userId);
    }

    @Test
    void clearTokens_shouldSkipUpdate_whenUserNotFound() {
        // Given
        Long userId = 1L;
        when(userMapper.selectById(userId)).thenReturn(null);
        doNothing().when(jwtProvider).clearTokens(userId);

        // When
        userService.clearTokens(userId);

        // Then
        verify(jwtProvider).clearTokens(userId);
    }

    // ===== updateLoginTime() tests =====

    @Test
    void updateLoginTime_shouldUpdateLastLoginTime() {
        // Given
        Long userId = 1L;
        User user = new User();
        user.setId(1L);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userMapper.update(any())).thenReturn(1);

        // When
        userService.updateLoginTime(userId);

        // Then
        verify(userMapper).update(any());
    }

    @Test
    void updateLoginTime_shouldSkipUpdate_whenUserNotFound() {
        // Given
        Long userId = 1L;
        when(userMapper.selectById(userId)).thenReturn(null);

        // When
        userService.updateLoginTime(userId);

        // Then
        verify(userMapper, never()).update(any());
    }
}