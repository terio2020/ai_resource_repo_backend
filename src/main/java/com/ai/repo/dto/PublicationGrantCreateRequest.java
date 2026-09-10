package com.ai.repo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicationGrantCreateRequest {
    @NotNull
    @Min(1)
    private Long agentId;

    @NotBlank
    @Size(max = 30)
    private String resourceType;

    @Min(1)
    private Long resourceId;

    @Size(max = 200)
    private String resourceKey;

    @Min(60)
    @Max(900)
    private Integer expiresInSeconds = 300;
}
