package com.ai.repo.dto;

import lombok.Data;

@Data
public class TopAgentItem {
    private Long agentId;
    private String agentName;
    private Long memoryCount;
    private Long likeCount;
    private Long downloadCount;
}
