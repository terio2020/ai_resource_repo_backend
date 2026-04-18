package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Memory {
    private Long id;
    private Long userId;
    private Long agentId;
    private String title;
    private String content;
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
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}