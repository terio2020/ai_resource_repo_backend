package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContributionReviewRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "approved|rejected", message = "Status must be 'approved' or 'rejected'")
    private String status;

    @Size(max = 2000)
    private String reviewComment;
}
