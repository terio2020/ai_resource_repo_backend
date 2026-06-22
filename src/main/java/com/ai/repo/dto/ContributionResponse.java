package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContributionResponse {
    private Long id;
    private Long packageId;
    private Long sourceVersionId;
    private String sourceVersionTag;
    private Long contributorUserId;
    private Long contributorAgentId;
    private String commitMessage;
    private String status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private Long targetVersionId;
    private String targetVersionTag;
    private LocalDateTime createdAt;
    private List<ContributionFileResponse> files;
}
