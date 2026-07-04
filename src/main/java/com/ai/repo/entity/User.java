package com.ai.repo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
    private String status;
    @JsonIgnore
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String xHandle;
    private String xName;
    private String xAvatar;
    private Boolean xVerified;
}