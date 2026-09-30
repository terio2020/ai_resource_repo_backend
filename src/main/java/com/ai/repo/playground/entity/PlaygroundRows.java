package com.ai.repo.playground.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

public final class PlaygroundRows {
    private PlaygroundRows() {}
    @Data public static class MatchEntry {
        private Long agentId, userId, permissionVersion, activityId;
        private String mode, briefJson, status;
        private int retries;
        private LocalDateTime createdAt, expiresAt;
    }
    @Data public static class Participation {
        private Long agentId, userId, version;
        private boolean enabled;
        private int maxDecisions, maxAttempts, maxDailyAttempts;
        private LocalDateTime updatedAt;
    }
    @Data public static class Activity {
        private Long id, hostAgentId, guestAgentId, nextSequence;
        private int horizonMonths;
        private String status, stateJson;
        private LocalDateTime expiresAt, createdAt, updatedAt;
    }
    @Data public static class Share {
        private Long activityId, removedByUserId;
        private String payloadJson, publicToken, removedReason;
        private LocalDateTime createdAt, removedAt;
    }
    @Data public static class Seat {
        private Long agentId, activityId, permissionVersion;
        private int decisionsUsed, attemptsUsed;
    }
    @Data public static class Task {
        private Long id, activityId, agentId, permissionVersion, attemptId;
        private String status, phase, leaseHash;
        private LocalDateTime expiresAt, leaseExpiresAt, createdAt;
    }
    @Data public static class Attempt {
        private Long id, taskId, agentId;
        private String requestKey, leaseHash, status, failureReason;
        private LocalDateTime createdAt;
    }
    @Data public static class Receipt {
        private Long agentId, taskId;
        private String requestKey, requestHash, responseJson;
        private LocalDateTime createdAt;
    }
    @Data public static class DailyBudget {
        private Long agentId;
        private LocalDate budgetDate;
        private int attemptsUsed;
    }
}
