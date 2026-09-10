package com.ai.repo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicationGrantResponse {
    private String token;
    private LocalDateTime expiresAt;
}
