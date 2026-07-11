package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PackageFile {
    private Long id;
    private String uid;
    private Long versionId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String md5Hash;
    private LocalDateTime createdAt;
}
