package com.ai.repo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SkillUpdateRequest {
    private Long userId;

    private Long agentId;

    @Size(max = 100, message = "Skill name must be less than 100 characters")
    private String name;

    @Size(max = 20, message = "Version must be less than 20 characters")
    private String version;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @Size(max = 500, message = "File path must be less than 500 characters")
    private String filePath;

    private Long fileSize;

    @Size(max = 100, message = "MIME type must be less than 100 characters")
    private String mimeType;

    private List<String> tags;

    @Size(max = 50, message = "Category must be less than 50 characters")
    private String category;

    @Size(max = 50, message = "Type must be less than 50 characters")
    private String type;

    private Boolean enabled;

    private Boolean isPublic;
}