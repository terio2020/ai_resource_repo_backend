package com.ai.repo.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class ConfigUpdateRequest {
    @Size(max = 10000, message = "Agent config must be less than 10000 characters")
    private String config;
}
