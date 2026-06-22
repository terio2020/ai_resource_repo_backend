package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @deprecated Replaced by AgentPackage + PackageVersion + PackageFile system.
 * Use /api/packages endpoints instead of the legacy file upload endpoints.
 */
@Deprecated
@Data
public class FileUploadLog {
    private Long id;
    private Long userId;
    private Long agentId;
    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadTime;
    private LocalDateTime createdAt;
}