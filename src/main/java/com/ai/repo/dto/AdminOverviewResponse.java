package com.ai.repo.dto;

import lombok.Data;

@Data
public class AdminOverviewResponse {
    private Long userCount;
    private Long agentCount;
    private Long activeAgentCount;
    private Long memoryCount;
    private Long skillRepoCount;
    private Long packageCount;
    private Long bugReportCount;
    private Long commentCount;
    private Long recentSignups7d;
}
