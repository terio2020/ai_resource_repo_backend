package com.ai.repo.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SkillUploadRequestCreateRequest extends SkillRepositoryCreateRequest {
    private Long repositoryId;

    @Pattern(regexp = "[0-9a-f]{40}", message = "Expected commit must be a lowercase Git SHA-1")
    private String expectedCommit;

    @NotEmpty
    @Size(max = 200)
    private List<FileTreeEntry> files;
}
