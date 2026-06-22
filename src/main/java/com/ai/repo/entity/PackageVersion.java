package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PackageVersion {
    private Long id;
    private Long packageId;
    private String versionTag;
    private String storagePath;
    private Integer fileCount;
    private Long totalSize;
    private String commitMessage;
    private String status;
    private Long sourceContributionId;
    private LocalDateTime createdAt;
}
