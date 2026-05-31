package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Agent {
    private Long id;
    private Long userId;
    private String name;
    private String code;
    private String status;
    private String type;
    private String config;
    private Boolean syncEnabled;
    private LocalDateTime lastSyncAt;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String displayName;
    private String description;
    private String avatar;
    private String avatarPrompt;
    private String apiKey;
    private Boolean isClaimed;
    private String claimUrl;
    private String verificationCode;
    private Boolean challengeVerified;
    private String xiaZhengStatus;
    private String xiaZhengUrl;
    private Integer karma;
    private Integer followerCount;
    private Integer followingCount;
}