package com.ai.repo.service.impl;

import com.ai.repo.entity.SocialAccount;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SocialAccountMapper;
import com.ai.repo.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class SocialAccountServiceImplTest {

    @Mock
    private SocialAccountMapper socialAccountMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    private SocialAccountServiceImpl socialAccountService;

    @BeforeEach
    void setUp() throws Exception {
        socialAccountService = new SocialAccountServiceImpl();
        setField("socialAccountMapper", socialAccountMapper);
        setField("userMapper", userMapper);
        setField("tokenEncryptionService", tokenEncryptionService);

        // Pass-through encrypt/decrypt so existing test assertions on token values still match
        lenient().when(tokenEncryptionService.encrypt(any())).thenAnswer((Answer<String>) inv -> inv.getArgument(0));
        lenient().when(tokenEncryptionService.decrypt(any())).thenAnswer((Answer<String>) inv -> inv.getArgument(0));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = SocialAccountServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(socialAccountService, value);
    }

    private SocialAccount sampleSa(Long id) {
        SocialAccount sa = new SocialAccount();
        sa.setId(id);
        sa.setUserId(1L);
        sa.setProvider("google");
        sa.setProviderUserId("google_12345");
        sa.setAccessToken("access_token");
        sa.setRefreshToken("refresh_token");
        sa.setEmail("test@gmail.com");
        sa.setNickname("Test User");
        sa.setAvatar("https://avatar.url");
        sa.setCreatedAt(LocalDateTime.now());
        sa.setUpdatedAt(LocalDateTime.now());
        return sa;
    }

    private User sampleUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("testuser");
        u.setEmail("test@example.com");
        u.setStatus("ACTIVE");
        return u;
    }

    @Test
    void findByProviderAndProviderUserId_shouldReturnSocialAccount() {
        SocialAccount sa = sampleSa(1L);
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "google_12345")).thenReturn(sa);
        SocialAccount result = socialAccountService.findByProviderAndProviderUserId("google", "google_12345");
        assertNotNull(result);
        assertEquals("google", result.getProvider());
    }

    @Test
    void findByProviderAndProviderUserId_shouldReturnNull_whenNotFound() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "x")).thenReturn(null);
        assertNull(socialAccountService.findByProviderAndProviderUserId("google", "x"));
    }

    @Test
    void findByUserId_shouldReturnList() {
        when(socialAccountMapper.selectByUserId(1L)).thenReturn(List.of(sampleSa(1L)));
        assertEquals(1, socialAccountService.findByUserId(1L).size());
    }

    @Test
    void findByUserIdAndProvider_shouldReturnSocialAccount() {
        when(socialAccountMapper.selectByUserIdAndProvider(1L, "google")).thenReturn(sampleSa(1L));
        assertNotNull(socialAccountService.findByUserIdAndProvider(1L, "google"));
    }

    @Test
    void linkSocialAccountToNewUser_shouldCreateUserAndSocialAccount() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(null);
        when(userMapper.selectByUsername("google_gid")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(socialAccountMapper.insert(any(SocialAccount.class))).thenReturn(1);

        User result = socialAccountService.linkSocialAccountToNewUser(
                "google", "gid", "a@b.com", "Nick", "av", "at", "rt", 3600L);

        assertEquals("google_gid", result.getUsername());
        assertEquals("a@b.com", result.getEmail());
        assertEquals("Nick", result.getNickname());
        assertEquals("USER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        verify(userMapper).insert(any(User.class));
        verify(socialAccountMapper).insert(any(SocialAccount.class));
    }

    @Test
    void linkSocialAccountToNewUser_shouldThrow_whenAlreadyLinked() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid"))
                .thenReturn(sampleSa(1L));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialAccountService.linkSocialAccountToNewUser("google", "gid", null, null, null, "at", null, null));
        assertTrue(ex.getMessage().contains("already linked"));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void linkSocialAccountToNewUser_shouldHandleNullFields() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(null);
        when(userMapper.selectByUsername("google_gid")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(socialAccountMapper.insert(any(SocialAccount.class))).thenReturn(1);

        User result = socialAccountService.linkSocialAccountToNewUser(
                "google", "gid", null, null, null, "at", null, null);

        assertEquals("", result.getEmail());
        assertEquals("google_gid", result.getNickname());
        assertEquals("", result.getAvatar());
    }

    @Test
    void linkSocialAccountToExistingUser_shouldCreateSocialAccount() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(null);
        when(socialAccountMapper.selectByUserIdAndProvider(1L, "google")).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(sampleUser(1L));
        when(socialAccountMapper.insert(any(SocialAccount.class))).thenReturn(1);

        SocialAccount result = socialAccountService.linkSocialAccountToExistingUser(
                1L, "google", "gid", "a@b.com", "Nick", "av", "at", "rt", 3600L);

        assertEquals(1L, result.getUserId());
        assertEquals("google", result.getProvider());
        verify(socialAccountMapper).insert(any(SocialAccount.class));
    }

    @Test
    void linkSocialAccountToExistingUser_shouldThrow_whenLinkedToAnotherUser() {
        SocialAccount existing = sampleSa(1L);
        existing.setUserId(2L);
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(existing);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialAccountService.linkSocialAccountToExistingUser(1L, "google", "gid", null, null, null, "at", null, null));
        assertTrue(ex.getMessage().contains("already linked to another user"));
    }

    @Test
    void linkSocialAccountToExistingUser_shouldThrow_whenProviderAlreadyLinked() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(null);
        when(socialAccountMapper.selectByUserIdAndProvider(1L, "google")).thenReturn(sampleSa(1L));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialAccountService.linkSocialAccountToExistingUser(1L, "google", "gid", null, null, null, "at", null, null));
        assertTrue(ex.getMessage().contains("already linked to your account"));
    }

    @Test
    void linkSocialAccountToExistingUser_shouldThrow_whenUserNotFound() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(null);
        when(socialAccountMapper.selectByUserIdAndProvider(1L, "google")).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialAccountService.linkSocialAccountToExistingUser(1L, "google", "gid", null, null, null, "at", null, null));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void authenticateWithSocialAccount_shouldReturnUser() {
        SocialAccount sa = sampleSa(1L);
        User user = sampleUser(1L);
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(sa);
        when(userMapper.selectById(1L)).thenReturn(user);

        User result = socialAccountService.authenticateWithSocialAccount("google", "gid");
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void authenticateWithSocialAccount_shouldReturnNull_whenNoSocialAccount() {
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "x")).thenReturn(null);
        assertNull(socialAccountService.authenticateWithSocialAccount("google", "x"));
    }

    @Test
    void authenticateWithSocialAccount_shouldReturnNull_whenUserDeleted() {
        SocialAccount sa = sampleSa(1L);
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(sa);
        when(userMapper.selectById(1L)).thenReturn(null);
        assertNull(socialAccountService.authenticateWithSocialAccount("google", "gid"));
    }

    @Test
    void authenticateWithSocialAccount_shouldThrow_whenUserInactive() {
        SocialAccount sa = sampleSa(1L);
        User user = sampleUser(1L);
        user.setStatus("INACTIVE");
        when(socialAccountMapper.selectByProviderAndProviderUserId("google", "gid")).thenReturn(sa);
        when(userMapper.selectById(1L)).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialAccountService.authenticateWithSocialAccount("google", "gid"));
        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    void unlinkSocialAccount_shouldDeleteAndReturnTrue() {
        when(socialAccountMapper.selectByUserIdAndProvider(1L, "google")).thenReturn(sampleSa(1L));
        when(socialAccountMapper.deleteById(1L)).thenReturn(1);

        boolean result = socialAccountService.unlinkSocialAccount(1L, "google");
        assertTrue(result);
        verify(socialAccountMapper).deleteById(1L);
    }

    @Test
    void unlinkSocialAccount_shouldThrow_whenNotFound() {
        when(socialAccountMapper.selectByUserIdAndProvider(1L, "google")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialAccountService.unlinkSocialAccount(1L, "google"));
        assertTrue(ex.getMessage().contains("Social account not found"));
    }

    @Test
    void updateTokens_shouldUpdate_whenFound() {
        SocialAccount sa = sampleSa(1L);
        when(socialAccountMapper.selectById(1L)).thenReturn(sa);

        socialAccountService.updateTokens(1L, "new_at", "new_rt", 7200L);

        assertEquals("new_at", sa.getAccessToken());
        assertEquals("new_rt", sa.getRefreshToken());
        assertNotNull(sa.getTokenExpiresAt());
        verify(socialAccountMapper).update(sa);
    }

    @Test
    void updateTokens_shouldThrow_whenNotFound() {
        when(socialAccountMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialAccountService.updateTokens(999L, "at", "rt", null));
        assertTrue(ex.getMessage().contains("Social account not found"));
    }

    @Test
    void updateTokens_shouldHandleNullExpiresIn() {
        SocialAccount sa = sampleSa(1L);
        when(socialAccountMapper.selectById(1L)).thenReturn(sa);

        socialAccountService.updateTokens(1L, "new_at", "new_rt", null);

        assertNull(sa.getTokenExpiresAt());
        verify(socialAccountMapper).update(sa);
    }

    @Test
    void isProviderConfigured_shouldReturnTrue_whenClientIdSet() throws Exception {
        setField("googleClientId", "google-id");
        setField("githubClientId", "github-id");

        assertTrue(socialAccountService.isProviderConfigured("google"));
        assertTrue(socialAccountService.isProviderConfigured("github"));
        assertFalse(socialAccountService.isProviderConfigured("apple"));
        assertFalse(socialAccountService.isProviderConfigured("wechat"));
        assertFalse(socialAccountService.isProviderConfigured("unknown"));
    }

    @Test
    void isProviderConfigured_shouldReturnFalse_whenClientIdEmpty() throws Exception {
        setField("googleClientId", "");
        assertFalse(socialAccountService.isProviderConfigured("google"));
    }
}
