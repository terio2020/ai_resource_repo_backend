package com.ai.repo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class AgentCreateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Agent name is required")
    @Size(max = 100, message = "Agent name must be less than 100 characters")
    private String name;

    @NotBlank(message = "Agent code is required")
    @Size(max = 50, message = "Agent code must be less than 50 characters")
    private String code;

    @Size(max = 50, message = "Agent type must be less than 50 characters")
    private String type;

    private String config;
}
