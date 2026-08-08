package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminContentStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "VISIBLE|BANNED", message = "Status must be VISIBLE or BANNED")
    private String status;
}
