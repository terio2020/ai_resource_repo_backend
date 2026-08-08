package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminBugReportStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "open|in_progress|resolved|closed",
            message = "Status must be open, in_progress, resolved or closed")
    private String status;
}
