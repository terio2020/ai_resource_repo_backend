package com.ai.repo.service.impl;

import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.exception.FileStorageException;
import com.ai.repo.exception.FileTooLargeException;
import com.ai.repo.exception.InvalidFileTypeException;
import com.ai.repo.mapper.FileUploadLogMapper;
import com.ai.repo.service.FileStorageService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Autowired
    private FileUploadLogMapper fileUploadLogMapper;

    @Value("${file.storage.base-path:/data/logicoma-files}")
    private String basePath;

    @Value("${file.storage.max-size-mb:50}")
    private long maxFileSizeMB;

    @Value("${file.storage.allowed-extensions:.md}")
    private String allowedExtensions;

    @Override
    public FileUploadResponse saveFile(MultipartFile file, Long userId, Long agentId, String fileType, String description) {
        validateFileType(file);
        validateFileSize(file);

        try {
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename(), agentId, fileType);
            String relativePath = fileType + "/" + uniqueFileName;
            String fullPath = basePath + "/" + agentId + "/" + relativePath;

            Path targetPath = Paths.get(fullPath);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());

            /*FileUploadLog uploadLog = new FileUploadLog();
            uploadLog.setUserId(userId);
            uploadLog.setAgentId(agentId);
            uploadLog.setOriginalFileName(file.getOriginalFilename());
            uploadLog.setStoredFileName(uniqueFileName);
            uploadLog.setFilePath(relativePath);
            uploadLog.setFileType(fileType);
            uploadLog.setFileSize(file.getSize());
            uploadLog.setUploadTime(LocalDateTime.now());

            fileUploadLogMapper.insert(uploadLog);*/

            FileUploadResponse response = new FileUploadResponse();
//            response.setFileId(uploadLog.getId());
            response.setFilePath(relativePath);
            response.setFileName(file.getOriginalFilename());
            response.setFileSize(file.getSize());
            response.setUploadTime(LocalDateTime.now());

            return response;
        } catch (IOException e) {
            throw new FileStorageException("Failed to save file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void validateFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);

        assert extension != null;
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new InvalidFileTypeException("Only .md files are allowed. Got: " + extension);
        }
    }

    @Override
    public void validateFileSize(MultipartFile file) {
        long fileSizeMB = file.getSize() / (1024 * 1024);
        
        if (fileSizeMB > maxFileSizeMB) {
            throw new FileTooLargeException("File size exceeds maximum limit of " + maxFileSizeMB + "MB", maxFileSizeMB);
        }
    }

    @Override
    public String generateUniqueFileName(String originalFileName, Long agentId, String fileType) {
        String baseName = FilenameUtils.getBaseName(originalFileName);
        String extension = FilenameUtils.getExtension(originalFileName);

        return baseName + "." + extension;
    }

    @Override
    public Resource loadFileAsResource(Long fileId, Long userId) {
        FileUploadLog uploadLog = fileUploadLogMapper.selectById(fileId);
        
        if (uploadLog == null) {
            throw new FileStorageException("File not found with id: " + fileId);
        }
        
        if (!uploadLog.getUserId().equals(userId)) {
            throw new FileStorageException("You don't have permission to access this file");
        }
        
        try {
            String fullPath = basePath + "/" + uploadLog.getAgentId() + "/" + uploadLog.getFilePath();
            Path filePath = Paths.get(fullPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found or not readable: " + fullPath);
            }
        } catch (Exception e) {
            throw new FileStorageException("Failed to load file: " + fileId, e);
        }
    }

    @Override
    public void deleteFile(Long fileId, Long userId) {
        FileUploadLog uploadLog = fileUploadLogMapper.selectById(fileId);
        
        if (uploadLog == null) {
            throw new FileStorageException("File not found with id: " + fileId);
        }
        
        if (!uploadLog.getUserId().equals(userId)) {
            throw new FileStorageException("You don't have permission to delete this file");
        }
        
        try {
            String fullPath = basePath + "/" + uploadLog.getAgentId() + "/" + uploadLog.getFilePath();
            Path filePath = Paths.get(fullPath).normalize();
            
            Files.deleteIfExists(filePath);
            fileUploadLogMapper.deleteById(fileId, userId);
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete file: " + fileId, e);
        }
    }

    @Override
    public List<FileUploadLog> getFileList(Long agentId, String fileType, Long userId) {
        return fileUploadLogMapper.selectByAgentIdAndFileType(agentId, fileType);
    }

    @Override
    public FileUploadLog getFileUploadLog(Long fileId) {
        return fileUploadLogMapper.selectById(fileId);
    }
}