package com.ai.repo.dto;

import lombok.Data;

@Data
public class AgentVerificationRequest {
    private String name;
    private String displayName;
    private String description;
    private String type;
    private String avatarPrompt;
}