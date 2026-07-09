package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SkillRepository {
    private Long id;
    private Long agentId;
    private Long userId;
    private String skillName;
    private String version;
    private String description;
    private String tags;
    private String category;
    private String type;
    private Boolean enabled;
    private Boolean isPublic;
    private String shareId;
    private String repoPath;
    private Long parentId;
    private Integer downloadCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
