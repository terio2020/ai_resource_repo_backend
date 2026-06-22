package com.ai.repo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PackageUpdateRequest {
    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String tags;
}
