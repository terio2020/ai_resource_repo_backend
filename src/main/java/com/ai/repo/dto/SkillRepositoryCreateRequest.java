package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkillRepositoryCreateRequest {
    @NotBlank(message = "Skill name is required")
    @Size(max = 100, message = "Skill name must be less than 100 characters")
    private String skillName;

    @Size(max = 50, message = "Version must be less than 50 characters")
    private String version;

    @Size(max = 50, message = "Description must be 50 characters or fewer")
    private String description;

    @Size(max = 500, message = "Tags must be less than 500 characters")
    private String tags;

    @Size(max = 100, message = "Category must be less than 100 characters")
    private String category;

    @Size(max = 50, message = "Type must be less than 50 characters")
    private String type;
}
