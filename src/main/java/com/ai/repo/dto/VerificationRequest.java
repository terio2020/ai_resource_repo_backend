package com.ai.repo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VerificationRequest {
    private String verificationCode;
    private BigDecimal answer;
}