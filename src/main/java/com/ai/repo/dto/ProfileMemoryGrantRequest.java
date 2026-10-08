package com.ai.repo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProfileMemoryGrantRequest {
    @NotNull
    @Size(max = 100)
    private List<String> namespaces;
}
