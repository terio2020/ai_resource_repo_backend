package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PackageResponse {
    private Long id;
    private Long userId;
    private Long agentId;
    private String packageType;
    private String name;
    private String description;
    private String tags;
    private Boolean isPublic;
    private Long currentVersionId;
    private String currentVersionTag;
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
