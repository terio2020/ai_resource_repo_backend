package com.ai.repo.service.impl;

import com.ai.repo.dto.*;
import com.ai.repo.entity.*;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.*;
import com.ai.repo.service.ContentModerationService;
import com.ai.repo.service.PackageService;
import com.ai.repo.service.PackageStorageService;
import com.ai.repo.util.StoragePathResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PackageServiceImpl implements PackageService {

    @Value("${package.storage.base-path}")
    private String basePath;

    @Resource
    private AgentPackageMapper agentPackageMapper;

    @Resource
    private PackageVersionMapper packageVersionMapper;

    @Resource
    private PackageFileMapper packageFileMapper;

    @Resource
    private PackageDownloadMapper packageDownloadMapper;

    @Resource
    private PackageStorageService packageStorageService;

    @Resource
    private ContentModerationService contentModerationService;

    @Override
    @Transactional
    public AgentPackage create(Long userId, Long agentId, PackageCreateRequest request) {
        String safeName = StoragePathResolver.safeSegment(request.getName(), "packageName");
        String safeType = StoragePathResolver.safeSegment(request.getPackageType(), "packageType");

        AgentPackage existing = agentPackageMapper.selectByAgentIdAndTypeAndName(
                agentId, safeType, safeName);
        if (existing != null) {
            throw new BusinessException(409, "Package '" + safeName + "' already exists for this agent");
        }

        AgentPackage ap = new AgentPackage();
        ap.setUserId(userId);
        ap.setAgentId(agentId);
        ap.setPackageType(safeType);
        ap.setName(safeName);
        ap.setDescription(request.getDescription());
        ap.setTags(request.getTags());
        ap.setIsPublic(false);
        ap.setDownloadCount(0);
        ap.setCreatedAt(LocalDateTime.now());
        agentPackageMapper.insert(ap);
        return ap;
    }

    @Override
    @Transactional
    public AgentPackage update(Long packageId, Long userId, PackageUpdateRequest request) {
        AgentPackage ap = findOwnedPackage(packageId, userId);
        if (request.getDescription() != null) {
            ap.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            ap.setTags(request.getTags());
        }
        agentPackageMapper.update(ap);
        return agentPackageMapper.selectById(packageId);
    }

    @Override
    @Transactional
    public void delete(Long packageId, Long userId) {
        AgentPackage ap = findOwnedPackage(packageId, userId);
        List<PackageVersion> versions = packageVersionMapper.selectByPackageId(packageId);
        for (PackageVersion v : versions) {
            packageStorageService.deleteDirectory(v.getStoragePath());
        }
        agentPackageMapper.deleteById(packageId);
    }

    @Override
    public AgentPackage findById(Long packageId) {
        AgentPackage ap = agentPackageMapper.selectById(packageId);
        if (ap == null) {
            throw new BusinessException(404, "Package not found");
        }
        return ap;
    }

    @Override
    @Transactional
    public PackageVersionResponse publishVersion(Long packageId, Long userId, String commitMessage,
                                                  List<MultipartFile> files) {
        AgentPackage ap = findOwnedPackage(packageId, userId);

        if (files == null || files.isEmpty()) {
            throw new BusinessException(400, "At least one file is required");
        }

        // F4: moderate every uploaded file in memory BEFORE any disk write.
        // Previously moderation ran AFTER saveFiles, so disallowed content already
        // landed on disk; and once it threw, the transaction rollback did not
        // remove the physical files left behind.
        moderateMultipartFiles(files);

        int versionNum = packageVersionMapper.selectMaxVersionNum(packageId) + 1;
        String versionTag = PackageStorageServiceImpl.generateVersionTag(packageId, versionNum);

        String versionDir = packageStorageService.createVersionDirectory(
                basePath, ap.getUserId(), ap.getAgentId(), ap.getPackageType(), ap.getName(), versionTag);

        PackageVersion pv = new PackageVersion();
        pv.setPackageId(packageId);
        pv.setVersionTag(versionTag);
        pv.setStoragePath(versionDir);
        pv.setCommitMessage(commitMessage);
        pv.setStatus("active");
        pv.setCreatedAt(LocalDateTime.now());
        packageVersionMapper.insert(pv);

        if (ap.getCurrentVersionId() != null) {
            packageVersionMapper.updateStatus(ap.getCurrentVersionId(), "superseded");
        }

        List<PackageFile> packageFiles = packageStorageService.saveFiles(pv.getId(), versionDir, files);

        long totalSize = packageFiles.stream().mapToLong(PackageFile::getFileSize).sum();
        pv.setFileCount(packageFiles.size());
        pv.setTotalSize(totalSize);
        packageVersionMapper.updateStatus(pv.getId(), "active");
        packageFileMapper.batchInsert(packageFiles);

        agentPackageMapper.updateCurrentVersion(packageId, pv.getId());

        log.info("Version {} published for package {} by user {}", versionTag, packageId, userId);
        return toVersionResponse(pv, packageFiles);
    }

    /**
     * Read each uploaded file's bytes and pass them through the content moderation pipeline.
     * Throws {@link BusinessException} (wrapping the underlying {@code ContentModerationException})
     * for any flagged content, so the caller's {@code @Transactional} rolls back the DB before any
     * filesystem side effects occur.
     */
    private void moderateMultipartFiles(List<MultipartFile> files) {
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            try {
                contentModerationService.moderateContent(new String(file.getBytes()), name);
            } catch (IOException e) {
                throw new BusinessException(400, "Cannot read file for moderation: " + name);
            }
        }
    }

    @Override
    public List<PackageVersionResponse> getVersions(Long packageId) {
        findById(packageId);
        List<PackageVersion> versions = packageVersionMapper.selectByPackageId(packageId);
        return versions.stream().map(v -> toVersionResponse(v, null)).collect(Collectors.toList());
    }

    @Override
    public PackageVersionResponse getVersionDetail(Long versionId) {
        PackageVersion pv = packageVersionMapper.selectById(versionId);
        if (pv == null) {
            throw new BusinessException(404, "Version not found");
        }
        List<PackageFile> files = packageFileMapper.selectByVersionId(versionId);
        return toVersionResponse(pv, files);
    }

    @Override
    public List<PackageFileResponse> getVersionFiles(Long versionId) {
        PackageVersion pv = packageVersionMapper.selectById(versionId);
        if (pv == null) {
            throw new BusinessException(404, "Version not found");
        }
        List<PackageFile> files = packageFileMapper.selectByVersionId(versionId);
        return files.stream().map(this::toFileResponse).collect(Collectors.toList());
    }

    @Override
    public org.springframework.core.io.Resource downloadFile(Long fileId, Long userId, Long downloaderUserId, Long downloaderAgentId) {
        PackageFile pf = packageFileMapper.selectById(fileId);
        if (pf == null) {
            throw new BusinessException(404, "File not found");
        }
        PackageVersion pv = packageVersionMapper.selectById(pf.getVersionId());
        if (pv == null) {
            throw new BusinessException(404, "Version not found");
        }
        AgentPackage ap = agentPackageMapper.selectById(pv.getPackageId());
        checkDownloadPermission(ap, downloaderUserId, downloaderAgentId);

        recordDownload(pv.getPackageId(), pv.getId(), downloaderUserId, downloaderAgentId);
        return packageStorageService.loadFileAsResource(pv.getStoragePath() + "/" + pf.getFilePath());
    }

    @Override
    public org.springframework.core.io.Resource downloadArchive(Long versionId, Long userId, Long downloaderUserId, Long downloaderAgentId) {
        PackageVersion pv = packageVersionMapper.selectById(versionId);
        if (pv == null) {
            throw new BusinessException(404, "Version not found");
        }
        AgentPackage ap = agentPackageMapper.selectById(pv.getPackageId());
        checkDownloadPermission(ap, downloaderUserId, downloaderAgentId);

        recordDownload(pv.getPackageId(), pv.getId(), downloaderUserId, downloaderAgentId);

        try {
            File zip = packageStorageService.packAsZip(pv.getStoragePath());
            return new org.springframework.core.io.FileSystemResource(zip);
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to create archive: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void setVisibility(Long packageId, Long userId, boolean isPublic) {
        AgentPackage ap = findOwnedPackage(packageId, userId);
        agentPackageMapper.updateVisibility(packageId, isPublic);
        log.info("Package {} visibility set to {} by user {}", packageId, isPublic, userId);
    }

    @Override
    @Transactional
    public void rollback(Long packageId, Long userId, Long targetVersionId) {
        AgentPackage ap = findOwnedPackage(packageId, userId);
        PackageVersion target = packageVersionMapper.selectById(targetVersionId);
        if (target == null || !target.getPackageId().equals(packageId)) {
            throw new BusinessException(404, "Target version not found in this package");
        }
        agentPackageMapper.updateCurrentVersion(packageId, targetVersionId);
        log.info("Package {} rolled back to version {} by user {}", packageId, targetVersionId, userId);
    }

    @Override
    public List<PackageResponse> listPublic(int page, int size) {
        List<AgentPackage> list = agentPackageMapper.selectPublic();
        return paginate(list, page, size);
    }

    @Override
    public List<PackageResponse> search(String keyword) {
        List<AgentPackage> list = agentPackageMapper.searchByKeyword(keyword);
        return list.stream().map(this::toPackageResponse).collect(Collectors.toList());
    }

    @Override
    public List<PackageResponse> listByAgent(Long agentId) {
        List<AgentPackage> list = agentPackageMapper.selectByAgentId(agentId);
        return list.stream().map(this::toPackageResponse).collect(Collectors.toList());
    }

    @Override
    public List<PackageResponse> listByUser(Long userId) {
        List<AgentPackage> list = agentPackageMapper.selectByUserId(userId);
        return list.stream().map(this::toPackageResponse).collect(Collectors.toList());
    }

    private AgentPackage findOwnedPackage(Long packageId, Long userId) {
        AgentPackage ap = findById(packageId);
        if (!ap.getUserId().equals(userId)) {
            throw new BusinessException(403, "Only the package owner can perform this action");
        }
        return ap;
    }

    private void checkDownloadPermission(AgentPackage ap, Long downloaderUserId, Long downloaderAgentId) {
        if (ap == null) {
            throw new BusinessException(404, "Package not found");
        }
        if (Boolean.TRUE.equals(ap.getIsPublic())) return;
        if (ap.getUserId().equals(downloaderUserId)) return;
        throw new BusinessException(403, "This package is private");
    }

    private void recordDownload(Long packageId, Long versionId, Long downloaderUserId, Long downloaderAgentId) {
        PackageDownload pd = new PackageDownload();
        pd.setPackageId(packageId);
        pd.setVersionId(versionId);
        pd.setDownloaderUserId(downloaderUserId);
        pd.setDownloaderAgentId(downloaderAgentId);
        pd.setCreatedAt(LocalDateTime.now());
        packageDownloadMapper.insert(pd);
        agentPackageMapper.incrementDownloadCount(packageId);
    }

    private PackageVersionResponse toVersionResponse(PackageVersion pv, List<PackageFile> files) {
        PackageVersionResponse r = new PackageVersionResponse();
        r.setId(pv.getId());
        r.setPackageId(pv.getPackageId());
        r.setVersionTag(pv.getVersionTag());
        r.setStoragePath(pv.getStoragePath());
        r.setFileCount(pv.getFileCount());
        r.setTotalSize(pv.getTotalSize());
        r.setCommitMessage(pv.getCommitMessage());
        r.setStatus(pv.getStatus());
        r.setSourceContributionId(pv.getSourceContributionId());
        r.setCreatedAt(pv.getCreatedAt());
        if (files != null) {
            r.setFiles(files.stream().map(this::toFileResponse).collect(Collectors.toList()));
        }
        return r;
    }

    private PackageFileResponse toFileResponse(PackageFile pf) {
        PackageFileResponse r = new PackageFileResponse();
        r.setId(pf.getId());
        r.setVersionId(pf.getVersionId());
        r.setFileName(pf.getFileName());
        r.setFilePath(pf.getFilePath());
        r.setFileSize(pf.getFileSize());
        r.setMimeType(pf.getMimeType());
        r.setMd5Hash(pf.getMd5Hash());
        r.setCreatedAt(pf.getCreatedAt());
        return r;
    }

    private PackageResponse toPackageResponse(AgentPackage ap) {
        PackageResponse r = new PackageResponse();
        r.setId(ap.getId());
        r.setUserId(ap.getUserId());
        r.setAgentId(ap.getAgentId());
        r.setPackageType(ap.getPackageType());
        r.setName(ap.getName());
        r.setDescription(ap.getDescription());
        r.setTags(ap.getTags());
        r.setIsPublic(ap.getIsPublic());
        r.setCurrentVersionId(ap.getCurrentVersionId());
        r.setDownloadCount(ap.getDownloadCount());
        r.setCreatedAt(ap.getCreatedAt());
        r.setUpdatedAt(ap.getUpdatedAt());

        if (ap.getCurrentVersionId() != null) {
            PackageVersion cv = packageVersionMapper.selectById(ap.getCurrentVersionId());
            if (cv != null) {
                r.setCurrentVersionTag(cv.getVersionTag());
            }
        }
        return r;
    }

    private List<PackageResponse> paginate(List<AgentPackage> list, int page, int size) {
        int start = (page - 1) * size;
        if (start >= list.size()) return Collections.emptyList();
        int end = Math.min(start + size, list.size());
        return list.subList(start, end).stream().map(this::toPackageResponse).collect(Collectors.toList());
    }
}
