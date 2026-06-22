package com.ai.repo.service;

import com.ai.repo.dto.*;
import com.ai.repo.entity.AgentPackage;
import com.ai.repo.entity.PackageVersion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PackageService {
    AgentPackage create(Long userId, Long agentId, PackageCreateRequest request);
    AgentPackage update(Long packageId, Long userId, PackageUpdateRequest request);
    void delete(Long packageId, Long userId);
    AgentPackage findById(Long packageId);

    PackageVersionResponse publishVersion(Long packageId, Long userId, String commitMessage, List<MultipartFile> files);
    List<PackageVersionResponse> getVersions(Long packageId);
    PackageVersionResponse getVersionDetail(Long versionId);

    List<PackageFileResponse> getVersionFiles(Long versionId);
    org.springframework.core.io.Resource downloadFile(Long fileId, Long userId, Long downloaderUserId, Long downloaderAgentId);
    org.springframework.core.io.Resource downloadArchive(Long versionId, Long userId, Long downloaderUserId, Long downloaderAgentId);

    void setVisibility(Long packageId, Long userId, boolean isPublic);
    void rollback(Long packageId, Long userId, Long targetVersionId);

    List<PackageResponse> listPublic(int page, int size);
    List<PackageResponse> search(String keyword);
    List<PackageResponse> listByAgent(Long agentId);
    List<PackageResponse> listByUser(Long userId);
}
