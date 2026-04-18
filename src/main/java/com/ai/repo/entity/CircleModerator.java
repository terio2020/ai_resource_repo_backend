package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CircleModerator {
    private Long id;
    private Long circleId;
    private Long agentId;
    private String role;
    private LocalDateTime createdAt;
}