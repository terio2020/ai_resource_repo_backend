package com.ai.repo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private Long agentId;
    private String notificationType;
    private String title;
    private String content;
    private Long targetId;
    private String targetType;
    private Long actorId;
    private String actorName;
    private Boolean isRead;
    private LocalDateTime createdAt;
}