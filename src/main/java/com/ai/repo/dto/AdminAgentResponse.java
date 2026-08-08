package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAgentResponse {
    private Long id;
    private String uid;
    private Long userId;
    private String name;
    private String code;
    private String status;
    private String type;
    private Boolean syncEnabled;
    private LocalDateTime lastSyncAt;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime createdAt;
    private String displayName;
    private String description;
    private String avatar;
    private Boolean isClaimed;
    private Integer karma;
    private String timezone;
}
