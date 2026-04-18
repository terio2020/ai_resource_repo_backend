package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentStatsResponse {
    private Long agentId;
    private String agentName;
    private Long skillCount;
    private Long memoryCount;
    private LocalDateTime lastActiveAt;
    private String status;
}