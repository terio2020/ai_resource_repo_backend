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
    private String category;
    private String tags;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}