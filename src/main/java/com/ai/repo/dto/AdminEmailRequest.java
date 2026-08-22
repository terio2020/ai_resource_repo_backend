package com.ai.repo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminEmailRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 160, message = "Subject must not exceed 160 characters")
    private String subject;

    @NotBlank(message = "Body is required")
    @Size(max = 10000, message = "Body must not exceed 10000 characters")
    private String body;

    @Size(max = 2048, message = "Action URL must not exceed 2048 characters")
    private String actionUrl;
}
