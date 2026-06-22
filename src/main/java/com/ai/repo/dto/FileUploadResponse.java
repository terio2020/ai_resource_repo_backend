package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @deprecated Replaced by package-based file management. See PackageFileResponse.
 */
@Deprecated
@Data
public class FileUploadResponse {
    private Long fileId;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadTime;
}