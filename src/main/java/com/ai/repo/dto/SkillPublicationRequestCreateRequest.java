package com.ai.repo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SkillPublicationRequestCreateRequest {
    @NotNull
    @Min(1)
    private Long repositoryId;
}
