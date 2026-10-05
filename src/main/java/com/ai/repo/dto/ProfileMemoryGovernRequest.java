package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileMemoryGovernRequest {
    @NotBlank
    private String action;
    private Object value;
    @Size(max = 500)
    private String note;
}
