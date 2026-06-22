package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PackageDownload {
    private Long id;
    private Long packageId;
    private Long versionId;
    private Long downloaderUserId;
    private Long downloaderAgentId;
    private LocalDateTime createdAt;
}
