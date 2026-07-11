package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PackageContribution {
    private Long id;
    private String uid;
    private Long packageId;
    private Long sourceVersionId;
    private Long contributorUserId;
    private Long contributorAgentId;
    private String commitMessage;
    private String status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private Long targetVersionId;
    private LocalDateTime createdAt;
}
