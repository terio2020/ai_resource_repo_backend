package com.ai.repo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkillRepositoryUpdateRequest {
    @Size(min = 1, max = 50, message = "Version must be between 1 and 50 characters")
    @Pattern(regexp = "(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)"
            + "(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?"
            + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?",
            message = "Version must use Semantic Versioning")
    private String version;

    @Size(min = 1, max = 50, message = "Description must be between 1 and 50 characters")
    private String description;

    @Size(min = 1, max = 500, message = "Tags must be between 1 and 500 characters")
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*(?:,[a-z0-9]+(?:-[a-z0-9]+)*){2,7}",
            message = "Tags must contain 3 to 8 lowercase comma-separated slugs")
    private String tags;

    @Size(max = 100, message = "Category must be less than 100 characters")
    private String category;

    @Size(max = 50, message = "Type must be less than 50 characters")
    private String type;

    private Boolean enabled;
}
