package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PackageVersionResponse {
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
    private List<PackageFileResponse> files;
}
