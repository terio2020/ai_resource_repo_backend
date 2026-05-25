package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.VerificationChallenge;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.VerifyChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/challenge")
@Tag(name = "Challenge API", description = "Agent challenge verification for API access")
public class VerifyChallengeController {

    @Resource
    private VerifyChallengeService verifyChallengeService;

    @GetMapping
    @ApiKeyAuth
    @Operation(summary = "Request a new challenge", description = "Agent requests a new challenge to verify. Must be completed within 5 minutes.")
    public Result<Map<String, Object>> requestChallenge(HttpServletRequest request) {
        Long agentId = (Long) request.getAttribute("agentId");
        if (agentId == null) {
            return Result.error(401, "Authentication required");
        }

        VerificationChallenge challenge = verifyChallengeService.requestChallenge(agentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("verificationCode", challenge.getVerificationCode());
        response.put("challengeText", challenge.getChallengeText());
        response.put("expiresAt", challenge.getExpiresAt());
        response.put("maxAttempts", challenge.getMaxAttempts());
        
        return Result.success(response);
    }

    @PostMapping("/verify")
    @ApiKeyAuth
    @Operation(summary = "Verify challenge answer", description = "Submit the answer to a challenge. 3 attempts max, 5 minute time limit.")
    public Result<Map<String, Object>> verifyAnswer(
            @RequestBody ChallengeVerifyRequest request,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        if (agentId == null) {
            return Result.error(401, "Authentication required");
        }

        boolean correct = verifyChallengeService.verifyAnswer(
                request.getVerificationCode(),
                request.getAnswer(),
                agentId
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("verified", correct);
        
        if (!correct) {
            response.put("message", "Incorrect answer");
            return Result.success(response);
        }
        
        response.put("message", "Challenge completed successfully");
        return Result.success(response);
    }

    @GetMapping("/status")
    @ApiKeyAuth
    @Operation(summary = "Check lockout status", description = "Check if the agent is currently locked out due to too many failed attempts")
    public Result<Map<String, Object>> checkStatus(HttpServletRequest request) {
        Long agentId = (Long) request.getAttribute("agentId");
        if (agentId == null) {
            return Result.error(401, "Authentication required");
        }

        boolean lockedOut = verifyChallengeService.isLockedOut(agentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("lockedOut", lockedOut);
        
        return Result.success(response);
    }

    public static class ChallengeVerifyRequest {
        private String verificationCode;
        private BigDecimal answer;

        public String getVerificationCode() {
            return verificationCode;
        }

        public void setVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
        }

        public BigDecimal getAnswer() {
            return answer;
        }

        public void setAnswer(BigDecimal answer) {
            this.answer = answer;
        }
    }
}