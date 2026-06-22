package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PackageFileResponse {
    private Long id;
    private Long versionId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String md5Hash;
    private LocalDateTime createdAt;
}
