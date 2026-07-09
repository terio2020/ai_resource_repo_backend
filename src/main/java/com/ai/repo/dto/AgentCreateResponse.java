package com.ai.repo.dto;

import lombok.Data;

@Data
public class AgentCreateResponse {
    private Long id;
    private Long userId;
    private String name;
    private String code;
    private String status;
    private String type;
    private String config;
    private String apiKey;
    private String apiKeyHash;
    private Boolean challengeVerified;
}
