package com.ai.repo.entity;

import lombok.Data;
import java.time.LocalDateTime;

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