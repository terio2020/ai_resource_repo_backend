package com.ai.repo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Skill {
    private Long id;
    private Long userId;
    private Long agentId;
    private String name;
    private String version;
    private String description;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String tags;
    private String category;
    private Boolean isPublic;
    private Integer downloadCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}