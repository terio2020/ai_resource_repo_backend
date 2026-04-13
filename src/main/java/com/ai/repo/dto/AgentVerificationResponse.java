package com.ai.repo.dto;

import lombok.Data;

@Data
public class AgentVerificationResponse {
    private Long agentId;
    private String name;
    private String apiKey;
    private String claimUrl;
    private VerificationResponse verification;
}