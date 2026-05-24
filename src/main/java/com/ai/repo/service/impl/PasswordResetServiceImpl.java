package com.ai.repo.service.impl;

import com.ai.repo.dto.PasswordResetConfirmRequest;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.PasswordResetService;
import com.ai.repo.service.UserService;
import com.ai.repo.util.PasswordEncoderUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String RESET_TOKEN_PREFIX = "password_reset:";
    private static final int TOKEN_SIZE_BYTES = 32;
    private static final long TOKEN_EXPIRE_MINUTES = 15;
    private static final long RATE_LIMIT_SECONDS = 60;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private PasswordEncoderUtil passwordEncoderUtil;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:noreply@logicoma.ai}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void requestPasswordReset(String email) {
        // Rate limiting - check if request was made recently
        String rateLimitKey = "password_reset_rate:" + email;
        Boolean exists = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(429, "Please wait before requesting another password reset email");
        }

        User user = userMapper.selectByEmail(email);
        
        // Security: Always return success to prevent account enumeration
        // Even if email doesn't exist, we don't reveal this
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", maskEmail(email));
            return;
        }

        // Generate reset token
        String token = generateSecureToken();
        String redisKey = RESET_TOKEN_PREFIX + token;
        
        // Store token in Redis with user ID
        redisTemplate.opsForValue().set(redisKey, user.getId().toString(), TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        // Set rate limit
        redisTemplate.opsForValue().set(rateLimitKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);

        // Send email
        try {
            sendResetEmail(user.getEmail(), token, user.getUsername());
            log.info("Password reset email sent to: {}", maskEmail(email));
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", maskEmail(email), e);
            // Don't expose email failure to user
        }
    }

    @Override
    public boolean validateResetToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String redisKey = RESET_TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();
        
        String redisKey = RESET_TOKEN_PREFIX + token;
        Object userIdObj = redisTemplate.opsForValue().get(redisKey);
        
        if (userIdObj == null) {
            throw new BusinessException(400, "Invalid or expired reset token");
        }
        
        Long userId;
        try {
            userId = Long.parseLong(userIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "Invalid reset token format");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }

        // Update password
        String encodedPassword = passwordEncoderUtil.encode(newPassword);
        user.setPassword(encodedPassword);
        userMapper.update(user);
        
        // Delete token (one-time use)
        redisTemplate.delete(redisKey);
        
        // Invalidate all existing sessions
        invalidateUserSessions(userId);
        
        // Send notification email about password change
        try {
            sendPasswordChangedNotification(user.getEmail(), user.getUsername());
            log.info("Password changed notification sent to: {}", maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send password change notification to: {}", maskEmail(user.getEmail()), e);
        }
        
        log.info("Password reset completed for user: {}", user.getUsername());
    }

    @Override
    public void invalidateUserSessions(Long userId) {
        userService.clearTokens(userId);
        log.info("Invalidated all sessions for user ID: {}", userId);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_SIZE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetEmail(String toEmail, String token, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Reset Your Password - Logicoma");
        
        String resetLink = baseUrl + "/api/users/password/reset-confirm?token=" + token;
        String emailBody = String.format(
            "Hi %s,\n\n" +
            "You requested a password reset for your Logicoma account.\n\n" +
            "Click the link below to set a new password:\n" +
            "%s\n\n" +
            "This link will expire in 15 minutes.\n\n" +
            "If you didn't request this, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Logicoma Team",
            username, resetLink
        );
        
        message.setText(emailBody);
        javaMailSender.send(message);
    }

    private void sendPasswordChangedNotification(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Password Was Changed - Logicoma");
        
        String emailBody = String.format(
            "Hi %s,\n\n" +
            "Your password was successfully changed.\n\n" +
            "If this was you, no further action is needed.\n\n" +
            "If this wasn't you, please contact us immediately.\n\n" +
            "Best regards,\n" +
            "Logicoma Team",
            username
        );
        
        message.setText(emailBody);
        javaMailSender.send(message);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        int len = local.length();
        if (len <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(len - 1) + "@" + domain;
    }
}