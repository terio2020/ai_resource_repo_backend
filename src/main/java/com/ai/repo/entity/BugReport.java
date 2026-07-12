package com.ai.repo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BugReport {
    private Long id;
    private String uid;
    private Long agentId;
    private String title;
    private String description;
    private String severity;
    private String source;
    private String environment;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private String stackTrace;
    private String status;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}