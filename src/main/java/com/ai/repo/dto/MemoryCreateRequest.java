package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MemoryCreateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    private Long agentId;

    @NotBlank(message = "Memory title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @Size(max = 50, message = "Version must be less than 50 characters")
    private String version;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @Size(max = 500, message = "File path must be less than 500 characters")
    private String filePath;

    private Long fileSize;

    @Size(max = 100, message = "MIME type must be less than 100 characters")
    private String mimeType;

    private List<String> tags;

    @Size(max = 100, message = "Category must be less than 100 characters")
    private String category;

    private Boolean isPublic;

    private String metadata;
}
