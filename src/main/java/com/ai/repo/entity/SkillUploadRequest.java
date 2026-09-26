package com.ai.repo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SkillUploadRequest {
    private Long id;
    private String tokenHash;
    private Long userId;
    private Long agentId;
    private Long repositoryId;
    private String skillName;
    private String version;
    private String description;
    private String tags;
    private String category;
    private String type;
    private String expectedCommit;
    private String manifestJson;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
