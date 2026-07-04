package com.ai.repo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SocialAccount {
    private Long id;
    private Long userId;
    private String provider;          // google, github, apple, wechat, etc.
    private String providerUserId;  // User ID from the social provider
    @JsonIgnore
    private String accessToken;      // Encrypted access token
    @JsonIgnore
    private String refreshToken;     // Encrypted refresh token (if available)
    private String email;            // Email from social account (may be null)
    private String nickname;         // Display name from social account
    private String avatar;           // Avatar URL from social account
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}