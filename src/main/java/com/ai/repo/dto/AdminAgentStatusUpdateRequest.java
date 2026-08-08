package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminAgentStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|IDLE|BUSY|OFFLINE|DISABLED", message = "Invalid agent status")
    private String status;
}
