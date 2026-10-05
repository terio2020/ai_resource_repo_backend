package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileMemoryGrant {
    private Long id;
    private String uid;
    private Long userId;
    private Long agentId;
    private String namespace;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
