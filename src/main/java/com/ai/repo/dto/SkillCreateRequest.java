package com.ai.repo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class SkillCreateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    private Long agentId;

    @NotBlank(message = "Skill name is required")
    @Size(max = 100, message = "Skill name must be less than 100 characters")
    private String name;

    @Size(max = 20, message = "Version must be less than 20 characters")
    private String version;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotBlank(message = "File path is required")
    @Size(max = 500, message = "File path must be less than 500 characters")
    private String filePath;

    private Long fileSize;

    @Size(max = 100, message = "MIME type must be less than 100 characters")
    private String mimeType;

    @Size(max = 500, message = "Tags must be less than 500 characters")
    private String tags;

    @Size(max = 50, message = "Category must be less than 50 characters")
    private String category;

    private Boolean isPublic;
}
