package com.ai.repo.service.impl;

import com.ai.repo.dto.*;
import com.ai.repo.entity.*;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.*;
import com.ai.repo.service.ContentModerationService;
import com.ai.repo.service.PackageContributionService;
import com.ai.repo.service.PackageStorageService;
import com.ai.repo.util.StoragePathResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class PackageContributionServiceImpl implements PackageContributionService {

    @Value("${package.storage.base-path}")
    private String basePath;

    @Resource
    private AgentPackageMapper agentPackageMapper;

    @Resource
    private PackageVersionMapper packageVersionMapper;

    @Resource
    private PackageFileMapper packageFileMapper;

    @Resource
    private PackageContributionMapper packageContributionMapper;

    @Resource
    private ContributionFileMapper contributionFileMapper;

    @Resource
    private PackageStorageService packageStorageService;

    @Resource
    private ContentModerationService contentModerationService;

    @Override
    @Transactional
    public ContributionResponse submit(Long packageId, Long userId, Long agentId,
                                        Long sourceVersionId, String commitMessage,
                                        List<MultipartFile> files) {
        AgentPackage ap = agentPackageMapper.selectById(packageId);
        if (ap == null) {
            throw new BusinessException(404, "Package not found");
        }
        if (!Boolean.TRUE.equals(ap.getIsPublic())) {
            throw new BusinessException(400, "Cannot contribute to a private package");
        }

        PackageVersion sourceVersion = packageVersionMapper.selectById(sourceVersionId);
        if (sourceVersion == null || !sourceVersion.getPackageId().equals(packageId)) {
            throw new BusinessException(404, "Source version not found");
        }

        if (userId != null && userId.equals(ap.getUserId())) {
            throw new BusinessException(400, "Cannot contribute to your own package");
        }

        // F5: moderate every contribution file in memory BEFORE any disk write.
        // Without this, a contributor could upload disallowed content (markdown images,
        // XSS, private-IP SSRF) that the package owner would see only after opening the
        // merged version — and the file would already be persisted on the API server.
        moderateMultipartFiles(files);

        PackageContribution pc = new PackageContribution();
        pc.setPackageId(packageId);
        pc.setSourceVersionId(sourceVersionId);
        pc.setContributorUserId(userId);
        pc.setContributorAgentId(agentId);
        pc.setCommitMessage(commitMessage);
        pc.setStatus("pending");
        pc.setCreatedAt(LocalDateTime.now());
        packageContributionMapper.insert(pc);

        String tempDir = basePath + "/contributions/tmp_" + pc.getId();
        try {
            Files.createDirectories(Path.of(tempDir));
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to create temp directory");
        }

        List<ContributionFile> contribFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String storagePath = packageStorageService.saveContributionFile(pc.getId(), tempDir, file);

            String fileName = file.getOriginalFilename();
            String md5 = computeMd5(file);

            ContributionFile cf = new ContributionFile();
            cf.setContributionId(pc.getId());
            cf.setFileName(fileName);
            cf.setFilePath(fileName);
            cf.setFileSize(file.getSize());
            cf.setMd5Hash(md5);
            cf.setStoragePath(storagePath);
            cf.setAction("modified");
            cf.setCreatedAt(LocalDateTime.now());
            contribFiles.add(cf);
        }
        contributionFileMapper.batchInsert(contribFiles);

        log.info("Contribution {} submitted for package {} by user {}", pc.getId(), packageId, userId);
        return toResponse(pc, contribFiles, ap, sourceVersion, null);
    }

    @Override
    @Transactional
    public ContributionResponse review(Long packageId, Long contributionId, Long reviewerId,
                                        ContributionReviewRequest request) {
        AgentPackage ap = agentPackageMapper.selectById(packageId);
        if (ap == null) {
            throw new BusinessException(404, "Package not found");
        }
        if (!ap.getUserId().equals(reviewerId)) {
            throw new BusinessException(403, "Only the package owner can review contributions");
        }

        PackageContribution pc = packageContributionMapper.selectById(contributionId);
        if (pc == null || !pc.getPackageId().equals(packageId)) {
            throw new BusinessException(404, "Contribution not found");
        }
        if (!"pending".equals(pc.getStatus())) {
            throw new BusinessException(400, "Contribution is already " + pc.getStatus());
        }

        pc.setReviewedBy(reviewerId);
        pc.setReviewedAt(LocalDateTime.now());
        pc.setReviewComment(request.getReviewComment());

        if ("approved".equals(request.getStatus())) {
            PackageVersion sourceVersion = packageVersionMapper.selectById(pc.getSourceVersionId());

            int versionNum = packageVersionMapper.selectMaxVersionNum(packageId) + 1;
            String versionTag = PackageStorageServiceImpl.generateVersionTag(packageId, versionNum);

            String newVersionDir = packageStorageService.createVersionDirectory(
                    basePath, ap.getUserId(), ap.getAgentId(), ap.getPackageType(), ap.getName(), versionTag);

            try {
                packageStorageService.copyDirectory(sourceVersion.getStoragePath(), newVersionDir);
            } catch (IOException e) {
                throw new BusinessException(500, "Failed to copy base version files: " + e.getMessage());
            }

            List<ContributionFile> contribFiles = contributionFileMapper.selectByContributionId(contributionId);
            for (ContributionFile cf : contribFiles) {
                try {
                    Path src = Path.of(cf.getStoragePath());
                    // F3: cf.getFilePath() came from the contributor's originalFilename and is
                    // joined onto newVersionDir; force it to stay a relative path under that dir.
                    String safeRelative = StoragePathResolver.safeRelativePath(cf.getFilePath(), "filePath");
                    Path dest = Path.of(newVersionDir, safeRelative);
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BusinessException(500, "Failed to merge contribution file: " + cf.getFilePath());
                }
            }

            // F5: re-moderate the merged directory contents before persisting metadata.
            // Defense in depth: contribution files were moderated at submit, but the source
            // version's pre-existing files were never moderated on this path. A reviewer
            // approving the PR should not be able to publish previously-bypassed disallowed
            // content as a new "active" version.
            moderateDirectoryContents(newVersionDir);

            PackageVersion newVersion = new PackageVersion();
            newVersion.setPackageId(packageId);
            newVersion.setVersionTag(versionTag);
            newVersion.setStoragePath(newVersionDir);
            newVersion.setCommitMessage("Merge contribution #" + contributionId + ": " + pc.getCommitMessage());
            newVersion.setStatus("active");
            newVersion.setSourceContributionId(contributionId);
            newVersion.setCreatedAt(LocalDateTime.now());
            packageVersionMapper.insert(newVersion);

            List<PackageFile> mergedFiles = packageStorageService.saveFilesFromDirectory(
                    newVersion.getId(), newVersionDir);
            long totalSize = mergedFiles.stream().mapToLong(PackageFile::getFileSize).sum();
            newVersion.setFileCount(mergedFiles.size());
            newVersion.setTotalSize(totalSize);
            packageFileMapper.batchInsert(mergedFiles);

            if (ap.getCurrentVersionId() != null) {
                packageVersionMapper.updateStatus(ap.getCurrentVersionId(), "superseded");
            }
            agentPackageMapper.updateCurrentVersion(packageId, newVersion.getId());

            pc.setTargetVersionId(newVersion.getId());
            pc.setStatus("merged");
            packageContributionMapper.update(pc);

            packageStorageService.deleteDirectory(basePath + "/contributions/tmp_" + contributionId);

            log.info("Contribution {} approved, merged as version {} for package {}",
                    contributionId, versionTag, packageId);
        } else {
            pc.setStatus("rejected");
            packageContributionMapper.update(pc);
            packageStorageService.deleteDirectory(basePath + "/contributions/tmp_" + contributionId);
            log.info("Contribution {} rejected for package {}", contributionId, packageId);
        }

        return toResponse(pc, contributionFileMapper.selectByContributionId(contributionId),
                          ap, packageVersionMapper.selectById(pc.getSourceVersionId()),
                          pc.getTargetVersionId() != null ? packageVersionMapper.selectById(pc.getTargetVersionId()) : null);
    }

    @Override
    public ContributionResponse getById(Long contributionId) {
        PackageContribution pc = packageContributionMapper.selectById(contributionId);
        if (pc == null) {
            throw new BusinessException(404, "Contribution not found");
        }
        List<ContributionFile> files = contributionFileMapper.selectByContributionId(contributionId);
        AgentPackage ap = agentPackageMapper.selectById(pc.getPackageId());
        return toResponse(pc, files, ap,
                packageVersionMapper.selectById(pc.getSourceVersionId()),
                pc.getTargetVersionId() != null ? packageVersionMapper.selectById(pc.getTargetVersionId()) : null);
    }

    @Override
    public List<ContributionResponse> listByPackage(Long packageId) {
        findPackage(packageId);
        List<PackageContribution> list = packageContributionMapper.selectByPackageId(packageId);
        AgentPackage ap = agentPackageMapper.selectById(packageId);
        return list.stream().map(pc -> {
            List<ContributionFile> files = contributionFileMapper.selectByContributionId(pc.getId());
            return toResponse(pc, files, ap,
                    packageVersionMapper.selectById(pc.getSourceVersionId()),
                    pc.getTargetVersionId() != null ? packageVersionMapper.selectById(pc.getTargetVersionId()) : null);
        }).collect(Collectors.toList());
    }

    private AgentPackage findPackage(Long packageId) {
        AgentPackage ap = agentPackageMapper.selectById(packageId);
        if (ap == null) {
            throw new BusinessException(404, "Package not found");
        }
        return ap;
    }

    private ContributionResponse toResponse(PackageContribution pc, List<ContributionFile> files,
                                              AgentPackage ap, PackageVersion sourceVersion,
                                              PackageVersion targetVersion) {
        ContributionResponse r = new ContributionResponse();
        r.setId(pc.getId());
        r.setPackageId(pc.getPackageId());
        r.setSourceVersionId(pc.getSourceVersionId());
        if (sourceVersion != null) {
            r.setSourceVersionTag(sourceVersion.getVersionTag());
        }
        r.setContributorUserId(pc.getContributorUserId());
        r.setContributorAgentId(pc.getContributorAgentId());
        r.setCommitMessage(pc.getCommitMessage());
        r.setStatus(pc.getStatus());
        r.setReviewedBy(pc.getReviewedBy());
        r.setReviewedAt(pc.getReviewedAt());
        r.setReviewComment(pc.getReviewComment());
        r.setTargetVersionId(pc.getTargetVersionId());
        if (targetVersion != null) {
            r.setTargetVersionTag(targetVersion.getVersionTag());
        }
        r.setCreatedAt(pc.getCreatedAt());

        if (files != null) {
            r.setFiles(files.stream().map(this::toFileResponse).collect(Collectors.toList()));
        }
        return r;
    }

    private ContributionFileResponse toFileResponse(ContributionFile cf) {
        ContributionFileResponse r = new ContributionFileResponse();
        r.setId(cf.getId());
        r.setContributionId(cf.getContributionId());
        r.setFileName(cf.getFileName());
        r.setFilePath(cf.getFilePath());
        r.setFileSize(cf.getFileSize());
        r.setMd5Hash(cf.getMd5Hash());
        r.setAction(cf.getAction());
        return r;
    }

    private String computeMd5(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (DigestInputStream dis = new DigestInputStream(file.getInputStream(), md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) { }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read each uploaded file's bytes and pass them through the content moderation pipeline.
     * Throws {@link BusinessException} (wrapping the underlying {@code ContentModerationException})
     * for any flagged content, so the caller's {@code @Transactional} rolls back the DB and
     * the caller never gets to the disk-write step.
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

    /**
     * Walk {@code dirPath} and moderate every regular file found, reading content from disk.
     * Throws {@link BusinessException} on read failure or on flagged content. Used on the
     * contribution {@code review(approve)} path after the contributor's files have been
     * merged on top of the source version's directory.
     */
    private void moderateDirectoryContents(String dirPath) {
        Path root = Path.of(dirPath);
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String content = new String(Files.readAllBytes(path));
                    contentModerationService.moderateContent(content, path.getFileName().toString());
                } catch (IOException e) {
                    throw new BusinessException(500, "Cannot read merged file for moderation: " + path);
                }
            });
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to walk merged version directory: " + e.getMessage());
        } catch (RuntimeException e) {
            // Re-throw BusinessException as-is; wrap anything else.
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(500, "Failed to moderate merged directory: " + e.getMessage());
        }
    }
}
