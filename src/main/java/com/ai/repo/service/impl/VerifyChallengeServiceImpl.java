package com.ai.repo.service.impl;

import com.ai.repo.entity.VerificationChallenge;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.VerificationChallengeMapper;
import com.ai.repo.service.VerifyChallengeService;
import com.ai.repo.util.UuidUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class VerifyChallengeServiceImpl implements VerifyChallengeService {

    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final int LOCKOUT_MINUTES = 30;

    @Resource
    private VerificationChallengeMapper challengeMapper;

    @Resource
    private AgentMapper agentMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public VerificationChallenge findByUid(String uid) {
        return challengeMapper.selectByUid(uid);
    }

    @Override
    @Transactional
    public VerificationChallenge requestChallenge(Long agentId) {
        // Check if agent is locked out
        if (isLockedOut(agentId)) {
            VerificationChallenge locked = challengeMapper.selectLockedByAgentId(agentId);
            long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), locked.getLockedUntil()).toMinutes();
            throw new BusinessException(429, "Too many failed attempts. Please try again in " + remainingMinutes + " minutes.");
        }

        // Generate new challenge using same pattern as VerificationServiceImpl
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setUid(UuidUtil.generate());
        challenge.setAgentId(agentId);
        challenge.setTargetType("agent_verification");
        
        String code = "verify_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        challenge.setVerificationCode(code);
        
        MathProblem problem = generateMathProblem();
        String obfuscatedText = obfuscateText(problem.text);
        
        challenge.setChallengeText(obfuscatedText);
        challenge.setAnswer(BigDecimal.valueOf(problem.answer));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(3); // Reduced to 3 for agent challenge
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setStatus("pending");
        challenge.setCreatedAt(LocalDateTime.now());
        challenge.setConsecutiveFailures(0);
        challenge.setLockedUntil(null);
        
        challengeMapper.insert(challenge);
        return challenge;
    }

    @Override
    @Transactional
    public boolean verifyAnswer(String verificationCode, BigDecimal answer, Long agentId) {
        VerificationChallenge challenge = challengeMapper.selectByCode(verificationCode);
        
        if (challenge == null) {
            throw new BusinessException(404, "Challenge not found");
        }
        
        // Verify this challenge belongs to the agent
        if (!challenge.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "Challenge does not belong to this agent");
        }
        
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            challengeMapper.updateStatus(challenge.getId(), "expired");
            throw new BusinessException(400, "Challenge expired");
        }
        
        if (challenge.getStatus().equals("verified")) {
            throw new BusinessException(400, "Challenge already completed");
        }
        
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            challengeMapper.updateStatus(challenge.getId(), "failed");
            handleFailedAttempt(agentId);
            throw new BusinessException(400, "Maximum attempts reached");
        }
        
        challengeMapper.incrementAttemptCount(challenge.getId());
        
        BigDecimal tolerance = new BigDecimal("0.01");
        if (answer.subtract(challenge.getAnswer()).abs().compareTo(tolerance) <= 0) {
            challengeMapper.updateStatus(challenge.getId(), "verified");
            resetConsecutiveFailures(agentId);
            agentMapper.updateChallengeVerified(agentId, true);
            return true;
        } else {
            // Wrong answer - check if this was the last attempt
            if (challenge.getAttemptCount() + 1 >= challenge.getMaxAttempts()) {
                challengeMapper.updateStatus(challenge.getId(), "failed");
                handleFailedAttempt(agentId);
            }
            return false;
        }
    }

    @Override
    public boolean isLockedOut(Long agentId) {
        VerificationChallenge locked = challengeMapper.selectLockedByAgentId(agentId);
        return locked != null && locked.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void handleFailedAttempt(Long agentId) {
        // Get current consecutive failures
        VerificationChallenge existingChallenge = challengeMapper.selectByAgentId(agentId).stream()
                .filter(c -> "locked".equals(c.getStatus()))
                .findFirst()
                .orElse(null);
        
        int currentFailures = 0;
        if (existingChallenge != null && existingChallenge.getConsecutiveFailures() != null) {
            currentFailures = existingChallenge.getConsecutiveFailures();
        }
        
        int newFailures = currentFailures + 1;
        
        if (newFailures >= MAX_CONSECUTIVE_FAILURES) {
            // Lock out for 30 minutes
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            challengeMapper.updateConsecutiveFailures(agentId, newFailures, lockedUntil);
            // Also update the challenge status to locked
            VerificationChallenge lockChallenge = challengeMapper.selectByAgentId(agentId).stream()
                    .filter(c -> "failed".equals(c.getStatus()))
                    .findFirst()
                    .orElse(null);
            if (lockChallenge != null) {
                challengeMapper.updateStatus(lockChallenge.getId(), "locked");
            }
        } else {
            // Just update consecutive failures, keep agent unlocked for retry
            challengeMapper.updateConsecutiveFailures(agentId, newFailures, null);
        }
    }

    private void resetConsecutiveFailures(Long agentId) {
        challengeMapper.updateConsecutiveFailures(agentId, 0, null);
    }

    private MathProblem generateMathProblem() {
        int operation = RANDOM.nextInt(3);
        int num1 = RANDOM.nextInt(50) + 1;
        int num2 = RANDOM.nextInt(50) + 1;
        
        String text;
        double answer;
        
        if (operation == 0) {
            text = "A basket has " + numberToWord(num1) + " apples and someone adds " + numberToWord(num2) + " more, how many apples total";
            answer = num1 + num2;
        } else if (operation == 1) {
            text = "A lobster swims at " + numberToWord(num1) + " meters and slows by " + numberToWord(num2) + ", what's the new speed";
            answer = num1 - num2;
        } else {
            text = "A treasure chest contains " + numberToWord(num1) + " coins, multiply by " + numberToWord(num2) + ", how many coins";
            answer = num1 * num2;
        }
        
        return new MathProblem(text, answer);
    }

    private String obfuscateText(String text) {
        StringBuilder result = new StringBuilder();
        String[] noiseChars = {"]", "^", "*", "|", "-", "~", "/", "["};
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == ' ') {
                if (RANDOM.nextBoolean()) {
                    result.append(noiseChars[RANDOM.nextInt(noiseChars.length)]);
                }
                result.append(' ');
                if (RANDOM.nextBoolean()) {
                    result.append(noiseChars[RANDOM.nextInt(noiseChars.length)]);
                }
            } else {
                if (RANDOM.nextBoolean()) {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(Character.toLowerCase(c));
                }
                
                if (RANDOM.nextDouble() < 0.15 && i < text.length() - 1) {
                    result.append(noiseChars[RANDOM.nextInt(noiseChars.length)]);
                }
            }
        }
        
        return result.toString();
    }

    private String numberToWord(int num) {
        String[] ones = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
                         "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", 
                         "seventeen", "eighteen", "nineteen"};
        String[] tens = {"", "", "twenty", "thirty", "forty", "fifty"};
        
        if (num < 20) {
            return ones[num];
        } else if (num < 60) {
            return tens[num / 10] + (num % 10 > 0 ? " " + ones[num % 10] : "");
        } else {
            return String.valueOf(num);
        }
    }

    private static class MathProblem {
        String text;
        double answer;
        
        MathProblem(String text, double answer) {
            this.text = text;
            this.answer = answer;
        }
    }
}