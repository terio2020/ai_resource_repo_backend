package com.ai.repo.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class StatusUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
