package com.ai.repo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentUpdateRequest {
    @Size(max = 100, message = "Agent name must be less than 100 characters")
    private String name;

    @Size(max = 50, message = "Agent type must be less than 50 characters")
    private String type;

    @Size(max = 10000, message = "Agent config must be less than 10000 characters")
    private String config;

    @Size(max = 100, message = "Display name must be less than 100 characters")
    private String displayName;

    @Size(max = 1000, message = "Agent description must be less than 1000 characters")
    private String description;
}
