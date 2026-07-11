package com.ai.repo.service;

import com.ai.repo.entity.VerificationChallenge;

public interface VerifyChallengeService {
    VerificationChallenge findByUid(String uid);

    /**
     * Request a new challenge for the agent.
     * Checks for lockout before generating.
     * @param agentId the agent requesting the challenge
     * @return the generated challenge
     * @throws BusinessException if agent is locked out
     */
    VerificationChallenge requestChallenge(Long agentId);

    /**
     * Verify the answer to a challenge.
     * Increments consecutive failures on wrong answer, resets on correct.
     * @param verificationCode the challenge code
     * @param answer the submitted answer
     * @param agentId the agent submitting the answer
     * @return true if answer is correct, false otherwise
     * @throws BusinessException if challenge not found, expired, or locked out
     */
    boolean verifyAnswer(String verificationCode, java.math.BigDecimal answer, Long agentId);

    /**
     * Check if an agent is currently locked out.
     * @param agentId the agent to check
     * @return true if locked out, false otherwise
     */
    boolean isLockedOut(Long agentId);
}