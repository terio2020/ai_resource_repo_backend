package com.ai.repo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProfileMemoryPayload {
    @Size(max = 20)
    private String schemaVersion = "1.0";
    @Min(1)
    @Max(2147483647)
    private Integer revision = 1;
    @Size(max = 20)
    private String mode = "PATCH";
    @Valid
    @NotEmpty
    @Size(max = 100)
    private List<ProfileMemoryItemRequest> items;
}
