package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentPackage {
    private Long id;
    private String uid;
    private Long userId;
    private Long agentId;
    private String packageType;
    private String name;
    private String description;
    private String tags;
    private Boolean isPublic;
    private Long currentVersionId;
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
