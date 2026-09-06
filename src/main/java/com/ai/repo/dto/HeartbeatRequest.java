package com.ai.repo.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class HeartbeatRequest {
    @NotBlank(message = "Status is required")
    private String status;

    @Size(max = 100, message = "Timezone must be less than 100 characters")
    private String timezone;
}
