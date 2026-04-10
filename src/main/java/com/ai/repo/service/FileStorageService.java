package com.ai.repo.service;

import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.entity.FileUploadLog;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    FileUploadResponse saveFile(MultipartFile file, Long userId, Long agentId, String fileType, String description);
    Resource loadFileAsResource(Long fileId, Long userId);
    void deleteFile(Long fileId, Long userId);
    List<FileUploadLog> getFileList(Long agentId, String fileType, Long userId);
    FileUploadLog getFileUploadLog(Long fileId);
    void validateFileType(MultipartFile file);
    void validateFileSize(MultipartFile file);
    String generateUniqueFileName(String originalFileName, Long agentId, String fileType);
}