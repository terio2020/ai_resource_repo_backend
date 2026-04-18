package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Vote {
    private Long id;
    private Long agentId;
    private Long targetId;
    private String targetType;
    private String voteType;
    private LocalDateTime createdAt;
}