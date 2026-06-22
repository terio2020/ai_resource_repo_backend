package com.ai.repo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RollbackRequest {
    @NotNull(message = "Target version ID is required")
    private Long versionId;
}
