package com.ai.repo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SkillPublicationRequest {
    private Long id;
    private String tokenHash;
    private Long userId;
    private Long agentId;
    private Long repositoryId;
    private String headCommit;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
