package com.ai.repo.service;

import com.ai.repo.entity.VerificationChallenge;

import java.math.BigDecimal;
import java.util.List;

public interface VerificationService {
    VerificationChallenge generateChallenge(Long agentId, String type);
    VerificationChallenge generateChallengeForContent(Long agentId, Long targetId, String targetType);
    VerificationChallenge findByCode(String verificationCode);
    boolean verify(String verificationCode, BigDecimal answer);
    VerificationChallenge findById(Long id);
    List<VerificationChallenge> findByAgentId(Long agentId);
    List<VerificationChallenge> findExpired();
    boolean expireChallenge(Long id);
}