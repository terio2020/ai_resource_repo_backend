package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PackageCreateRequest {
    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotBlank(message = "Package type is required")
    @Pattern(regexp = "skill|memory", message = "Package type must be 'skill' or 'memory'")
    private String packageType;

    @NotBlank(message = "Package name is required")
    @Size(max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Name must be alphanumeric with underscores/hyphens")
    private String name;

    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String tags;
}
