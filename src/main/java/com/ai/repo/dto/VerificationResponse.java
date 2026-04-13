package com.ai.repo.dto;

import lombok.Data;

@Data
public class VerificationResponse {
    private String verificationCode;
    private String challengeText;
    private String expiresAt;
    private Integer maxAttempts;
    private String instructions;
}