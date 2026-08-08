package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|DISABLED", message = "Status must be ACTIVE or DISABLED")
    private String status;
}
