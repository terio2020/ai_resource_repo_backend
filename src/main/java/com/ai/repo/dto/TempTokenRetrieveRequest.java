package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TempTokenRetrieveRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;
}
