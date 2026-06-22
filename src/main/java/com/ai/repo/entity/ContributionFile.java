package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContributionFile {
    private Long id;
    private Long contributionId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String md5Hash;
    private String storagePath;
    private String action;
    private LocalDateTime createdAt;
}
