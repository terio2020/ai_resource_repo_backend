package com.ai.repo.service;

import com.ai.repo.entity.PackageFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PackageStorageService {
    String createVersionDirectory(String basePath, Long userId, Long agentId, String packageType,
                                  String packageName, String versionTag);
    List<PackageFile> saveFiles(Long versionId, String versionDir, List<MultipartFile> files);
    Resource loadFileAsResource(String filePath);
    java.io.File packAsZip(String versionDir) throws IOException;
    String saveContributionFile(Long contributionId, String tempDir, MultipartFile file);
    void deleteDirectory(String dirPath);
    void copyDirectory(String sourceDir, String targetDir) throws IOException;
    List<PackageFile> saveFilesFromDirectory(Long versionId, String dirPath);
}
