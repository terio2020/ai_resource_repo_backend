package com.ai.repo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VerificationChallenge {
    private Long id;
    private Long agentId;
    private Long targetId;
    private String targetType;
    private String verificationCode;
    private String challengeText;
    private BigDecimal answer;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime expiresAt;
    private String status;
    private LocalDateTime createdAt;
}