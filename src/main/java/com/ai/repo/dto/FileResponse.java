package com.ai.repo.dto;

import com.ai.repo.entity.FileType;
import lombok.Data;

@Data
public class FileResponse {
    private Long id;
    private String name;
    private FileType type;
    private String filePath;
    private Long fileSize;
    private String content;
    private String description;
    private String tags;
    private String mimeType;
    private String createdAt;
    private String updatedAt;
}
