package com.ai.repo.service.impl;

import com.ai.repo.entity.VerificationChallenge;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.VerificationChallengeMapper;
import com.ai.repo.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VerificationServiceImpl implements VerificationService {

    @Autowired
    private VerificationChallengeMapper challengeMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public VerificationChallenge generateChallenge(Long agentId, String type) {
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setAgentId(agentId);
        challenge.setTargetType(type);
        
        String code = "verify_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        challenge.setVerificationCode(code);
        
        MathProblem problem = generateMathProblem();
        String obfuscatedText = obfuscateText(problem.text);
        
        challenge.setChallengeText(obfuscatedText);
        challenge.setAnswer(BigDecimal.valueOf(problem.answer));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(5);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setStatus("pending");
        challenge.setCreatedAt(LocalDateTime.now());
        
        challengeMapper.insert(challenge);
        return challenge;
    }

    @Override
    @Transactional
    public VerificationChallenge generateChallengeForContent(Long agentId, Long targetId, String targetType) {
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setAgentId(agentId);
        challenge.setTargetId(targetId);
        challenge.setTargetType(targetType);
        
        String code = "content_verify_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        challenge.setVerificationCode(code);
        
        MathProblem problem = generateMathProblem();
        String obfuscatedText = obfuscateText(problem.text);
        
        challenge.setChallengeText(obfuscatedText);
        challenge.setAnswer(BigDecimal.valueOf(problem.answer));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(5);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setStatus("pending");
        challenge.setCreatedAt(LocalDateTime.now());
        
        challengeMapper.insert(challenge);
        return challenge;
    }

    @Override
    public VerificationChallenge findByCode(String verificationCode) {
        return challengeMapper.selectByCode(verificationCode);
    }

    @Override
    @Transactional
    public boolean verify(String verificationCode, BigDecimal answer) {
        VerificationChallenge challenge = challengeMapper.selectByCode(verificationCode);
        
        if (challenge == null) {
            throw new BusinessException("Challenge not found");
        }
        
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            challengeMapper.updateStatus(challenge.getId(), "expired");
            throw new BusinessException("Challenge expired");
        }
        
        if (challenge.getStatus().equals("verified") || challenge.getStatus().equals("failed")) {
            throw new BusinessException("Challenge already completed");
        }
        
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            challengeMapper.updateStatus(challenge.getId(), "failed");
            throw new BusinessException("Maximum attempts reached");
        }
        
        challengeMapper.incrementAttemptCount(challenge.getId());
        
        BigDecimal tolerance = new BigDecimal("0.01");
        if (answer.subtract(challenge.getAnswer()).abs().compareTo(tolerance) <= 0) {
            challengeMapper.updateStatus(challenge.getId(), "verified");
            return true;
        } else {
            if (challenge.getAttemptCount() + 1 >= challenge.getMaxAttempts()) {
                challengeMapper.updateStatus(challenge.getId(), "failed");
            }
            return false;
        }
    }

    @Override
    public VerificationChallenge findById(Long id) {
        return challengeMapper.selectById(id);
    }

    @Override
    public List<VerificationChallenge> findByAgentId(Long agentId) {
        return challengeMapper.selectByAgentId(agentId);
    }

    @Override
    public List<VerificationChallenge> findExpired() {
        return challengeMapper.selectExpired();
    }

    @Override
    @Transactional
    public boolean expireChallenge(Long id) {
        VerificationChallenge challenge = challengeMapper.selectById(id);
        if (challenge == null) {
            return false;
        }
        if (challenge.getStatus().equals("pending")) {
            return challengeMapper.updateStatus(id, "expired") > 0;
        }
        return false;
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