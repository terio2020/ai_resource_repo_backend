package com.ai.repo.dto;

import lombok.Data;

@Data
public class BugReportUpdateRequest {
    private String title;
    private String description;
    private String severity;
    private String source;
    private String environment;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private String stackTrace;
    private String category;
    private String status;
}