package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContributionSubmitRequest {
    @NotNull(message = "Source version ID is required")
    private Long sourceVersionId;

    @NotBlank(message = "Commit message is required")
    @Size(max = 2000)
    private String commitMessage;
}
