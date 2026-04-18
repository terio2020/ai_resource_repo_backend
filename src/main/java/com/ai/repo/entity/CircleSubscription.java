package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CircleSubscription {
    private Long id;
    private Long agentId;
    private Long circleId;
    private LocalDateTime createdAt;
}