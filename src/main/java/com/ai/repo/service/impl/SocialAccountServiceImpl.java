package com.ai.repo.service.impl;

import com.ai.repo.entity.SocialAccount;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SocialAccountMapper;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.SocialAccountService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SocialAccountServiceImpl implements SocialAccountService {

    @Resource
    private SocialAccountMapper socialAccountMapper;

    @Resource
    private UserMapper userMapper;

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.github.client-id:}")
    private String githubClientId;

    @Value("${oauth.apple.client-id:}")
    private String appleClientId;

    @Value("${oauth.wechat.client-id:}")
    private String wechatClientId;

    @Override
    public SocialAccount findByProviderAndProviderUserId(String provider, String providerUserId) {
        return socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public List<SocialAccount> findByUserId(Long userId) {
        return socialAccountMapper.selectByUserId(userId);
    }

    @Override
    public SocialAccount findByUserIdAndProvider(Long userId, String provider) {
        return socialAccountMapper.selectByUserIdAndProvider(userId, provider);
    }

    @Override
    @Transactional
    public User linkSocialAccountToNewUser(String provider, String providerUserId, String email,
                                            String nickname, String avatar, String accessToken,
                                            String refreshToken, Long expiresIn) {
        // Check if social account already exists
        SocialAccount existing = socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
        if (existing != null) {
            throw new BusinessException(400, "Social account already linked to a user");
        }

        // Generate unique username from provider and providerUserId
        String baseUsername = provider + "_" + providerUserId;
        String username = generateUniqueUsername(baseUsername);
        if (username == null) {
            throw new BusinessException(500, "Failed to generate unique username");
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setPassword(""); // No password for social login users
        user.setEmail(email != null ? email : "");
        user.setNickname(nickname != null ? nickname : username);
        user.setAvatar(avatar != null ? avatar : "");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        
        userMapper.insert(user);
        log.info("Created new user via social login: {}, provider: {}", username, provider);

        // Create social account link
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUserId(user.getId());
        socialAccount.setProvider(provider);
        socialAccount.setProviderUserId(providerUserId);
        socialAccount.setAccessToken(accessToken);
        socialAccount.setRefreshToken(refreshToken);
        socialAccount.setEmail(email);
        socialAccount.setNickname(nickname);
        socialAccount.setAvatar(avatar);
        socialAccount.setTokenExpiresAt(expiresIn != null ? LocalDateTime.now().plusSeconds(expiresIn) : null);
        socialAccount.setCreatedAt(LocalDateTime.now());
        socialAccount.setUpdatedAt(LocalDateTime.now());
        
        socialAccountMapper.insert(socialAccount);
        log.info("Linked social account to user: {}, provider: {}", username, provider);

        return user;
    }

    @Override
    @Transactional
    public SocialAccount linkSocialAccountToExistingUser(Long userId, String provider,
                                                          String providerUserId, String email,
                                                          String nickname, String avatar,
                                                          String accessToken, String refreshToken,
                                                          Long expiresIn) {
        // Check if this social account is already linked to another user
        SocialAccount existing = socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
        if (existing != null && !existing.getUserId().equals(userId)) {
            throw new BusinessException(400, "This social account is already linked to another user");
        }

        // Check if user already has this provider linked
        SocialAccount userAccount = socialAccountMapper.selectByUserIdAndProvider(userId, provider);
        if (userAccount != null) {
            throw new BusinessException(400, "This provider is already linked to your account");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }

        // Create social account link
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUserId(userId);
        socialAccount.setProvider(provider);
        socialAccount.setProviderUserId(providerUserId);
        socialAccount.setAccessToken(accessToken);
        socialAccount.setRefreshToken(refreshToken);
        socialAccount.setEmail(email);
        socialAccount.setNickname(nickname);
        socialAccount.setAvatar(avatar);
        socialAccount.setTokenExpiresAt(expiresIn != null ? LocalDateTime.now().plusSeconds(expiresIn) : null);
        socialAccount.setCreatedAt(LocalDateTime.now());
        socialAccount.setUpdatedAt(LocalDateTime.now());
        
        socialAccountMapper.insert(socialAccount);
        log.info("Linked social account to existing user: {}, provider: {}", user.getUsername(), provider);

        return socialAccount;
    }

    @Override
    public User authenticateWithSocialAccount(String provider, String providerUserId) {
        SocialAccount socialAccount = socialAccountMapper.selectByProviderAndProviderUserId(provider, providerUserId);
        if (socialAccount == null) {
            return null;
        }
        
        User user = userMapper.selectById(socialAccount.getUserId());
        if (user == null) {
            log.warn("Social account {}:{} linked to non-existent user", provider, providerUserId);
            return null;
        }
        
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "User account is not active");
        }
        
        return user;
    }

    @Override
    @Transactional
    public boolean unlinkSocialAccount(Long userId, String provider) {
        SocialAccount socialAccount = socialAccountMapper.selectByUserIdAndProvider(userId, provider);
        if (socialAccount == null) {
            throw new BusinessException(404, "Social account not found");
        }
        
        socialAccountMapper.deleteById(socialAccount.getId());
        log.info("Unlinked social account: userId={}, provider={}", userId, provider);
        return true;
    }

    @Override
    @Transactional
    public void updateTokens(Long socialAccountId, String accessToken, String refreshToken, Long expiresIn) {
        SocialAccount socialAccount = socialAccountMapper.selectById(socialAccountId);
        if (socialAccount == null) {
            throw new BusinessException(404, "Social account not found");
        }
        
        socialAccount.setAccessToken(accessToken);
        socialAccount.setRefreshToken(refreshToken);
        socialAccount.setTokenExpiresAt(expiresIn != null ? LocalDateTime.now().plusSeconds(expiresIn) : null);
        socialAccount.setUpdatedAt(LocalDateTime.now());
        
        socialAccountMapper.update(socialAccount);
    }

    @Override
    public boolean isProviderConfigured(String provider) {
        switch (provider.toLowerCase()) {
            case "google":
                return googleClientId != null && !googleClientId.isEmpty();
            case "github":
                return githubClientId != null && !githubClientId.isEmpty();
            case "apple":
                return appleClientId != null && !appleClientId.isEmpty();
            case "wechat":
                return wechatClientId != null && !wechatClientId.isEmpty();
            default:
                return false;
        }
    }

    private String generateUniqueUsername(String baseUsername) {
        // baseUsername like "google_123456789"
        String username = baseUsername.length() > 50 ? baseUsername.substring(0, 50) : baseUsername;
        
        // Check if exists, if so add random suffix
        if (userMapper.selectByUsername(username) == null) {
            return username;
        }
        
        // Try with random suffix
        for (int i = 0; i < 10; i++) {
            String candidate = username + "_" + UUID.randomUUID().toString().substring(0, 8);
            if (userMapper.selectByUsername(candidate) == null) {
                return candidate;
            }
        }
        
        return null; // Failed after 10 attempts
    }
}