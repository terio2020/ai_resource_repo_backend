package com.ai.repo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private String apiKey;
    private String apiKeyHash;
    private Boolean isClaimed;
    @JsonIgnore
    private String claimUrl;
    @JsonIgnore
    private String verificationCode;
    private Boolean challengeVerified;
    private String xiaZhengStatus;
    @JsonIgnore
    private String xiaZhengUrl;
    private Integer karma;
}