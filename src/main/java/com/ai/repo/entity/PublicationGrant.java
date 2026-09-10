package com.ai.repo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PublicationGrant {
    private Long id;
    private String tokenHash;
    private Long userId;
    private Long agentId;
    private String resourceType;
    private Long resourceId;
    private String resourceKey;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;
}
