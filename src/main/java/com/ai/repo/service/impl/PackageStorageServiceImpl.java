package com.ai.repo.service.impl;

import com.ai.repo.entity.PackageFile;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.FileTooLargeException;
import com.ai.repo.service.PackageStorageService;
import com.ai.repo.util.StoragePathResolver;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class PackageStorageServiceImpl implements PackageStorageService {

    @Value("${package.storage.base-path}")
    private String basePath;

    @Value("${package.storage.allowed-extensions}")
    private String allowedExtensions;

    @Value("${package.storage.max-file-size-mb:50}")
    private long maxFileSizeMb;

    @Value("${package.storage.max-files-per-version:100}")
    private int maxFilesPerVersion;

    private List<String> allowedExtList;

    @PostConstruct
    public void init() {
        allowedExtList = List.of(allowedExtensions.split(","));
        try {
            Files.createDirectories(Path.of(basePath));
        } catch (IOException e) {
            log.warn("Cannot create package storage base directory: {}", basePath);
        }
    }

    @Override
    public String createVersionDirectory(String basePath, Long userId, Long agentId,
                                          String packageType, String packageName, String versionTag) {
        String safeType = StoragePathResolver.safeSegment(packageType, "packageType");
        String safeName = StoragePathResolver.safeSegment(packageName, "packageName");
        String safeVersion = StoragePathResolver.safeSegment(versionTag, "versionTag");
        String dir = basePath + "/" + safeType + "/" + userId + "/" + agentId + "/" + safeName + "/" + safeVersion;
        try {
            Files.createDirectories(Path.of(dir));
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to create version directory: " + e.getMessage());
        }
        return dir;
    }

    @Override
    public List<PackageFile> saveFiles(Long versionId, String versionDir, List<MultipartFile> files) {
        if (files.size() > maxFilesPerVersion) {
            throw new BusinessException(400, "Exceeded max files per version: " + maxFilesPerVersion);
        }

        List<PackageFile> result = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFileType(file);
            validateFileSize(file);

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                throw new BusinessException(400, "File name must not be empty");
            }

            String sanitized = StoragePathResolver.safeRelativePath(fileName, "fileName");
            Path targetPath = Path.of(versionDir, sanitized);

            try {
                Files.createDirectories(targetPath.getParent());
                String md5 = saveWithMd5(file, targetPath);
                long fileSize = file.getSize();
                String mimeType = file.getContentType();

                PackageFile pf = new PackageFile();
                pf.setVersionId(versionId);
                pf.setFileName(sanitized);
                pf.setFilePath(sanitized);
                pf.setFileSize(fileSize);
                pf.setMimeType(mimeType);
                pf.setMd5Hash(md5);
                pf.setCreatedAt(LocalDateTime.now());
                result.add(pf);
            } catch (IOException e) {
                throw new BusinessException(500, "Failed to save file: " + sanitized + " - " + e.getMessage());
            }
        }
        return result;
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(404, "File not found: " + filePath);
        }
        return new FileSystemResource(file);
    }

    @Override
    public File packAsZip(String versionDir) throws IOException {
        Path dirPath = Path.of(versionDir);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new BusinessException(404, "Version directory not found");
        }

        File zipFile = Files.createTempFile("package_", ".zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
             Stream<Path> walk = Files.walk(dirPath)) {
            walk.filter(Files::isRegularFile)
                .forEach(path -> {
                    String entryName = dirPath.relativize(path).toString().replace("\\", "/");
                    try {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to zip entry: " + entryName, e);
                    }
                });
        }
        return zipFile;
    }

    @Override
    public String saveContributionFile(Long contributionId, String tempDir, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(400, "File name must not be empty");
        }

        String sanitized = StoragePathResolver.safeRelativePath(fileName, "fileName");
        Path targetPath = Path.of(tempDir, sanitized);
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to save contribution file: " + e.getMessage());
        }
        return targetPath.toAbsolutePath().toString();
    }

    @Override
    public void deleteDirectory(String dirPath) {
        Path dir = Path.of(dirPath);
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> paths = walk.sorted((a, b) -> b.compareTo(a)).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("Failed to delete directory {}: {}", dirPath, e.getMessage());
        }
    }

    @Override
    public void copyDirectory(String sourceDir, String targetDir) throws IOException {
        Path source = Path.of(sourceDir);
        Path target = Path.of(targetDir);
        try (Stream<Path> stream = Files.walk(source)) {
            List<Path> sources = stream.toList();
            for (Path src : sources) {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static String generateVersionTag(Long packageId, int versionNum) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "v" + versionNum + "_" + ts;
    }

    private void validateFileType(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            throw new BusinessException(400, "File must have an extension");
        }
        String ext = name.substring(name.lastIndexOf('.')).toLowerCase();
        if (!allowedExtList.contains(ext)) {
            throw new BusinessException(400, "File type '" + ext + "' is not allowed. Allowed: " + allowedExtensions);
        }
        if (name.contains("..") || name.contains("\0") || name.startsWith(".")) {
            throw new BusinessException(400, "Invalid file name: " + name);
        }
    }

    private void validateFileSize(MultipartFile file) {
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new FileTooLargeException("File exceeds " + maxFileSizeMb + "MB limit", maxFileSizeMb);
        }
    }

    @Override
    public List<PackageFile> saveFilesFromDirectory(Long versionId, String dirPath) {
        List<PackageFile> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Path.of(dirPath))) {
            walk.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String relativePath = Path.of(dirPath).relativize(path).toString().replace("\\", "/");
                        String md5 = computeMd5(path);
                        PackageFile pf = new PackageFile();
                        pf.setVersionId(versionId);
                        pf.setFileName(path.getFileName().toString());
                        pf.setFilePath(relativePath);
                        pf.setFileSize(Files.size(path));
                        pf.setMd5Hash(md5);
                        pf.setCreatedAt(LocalDateTime.now());
                        result.add(pf);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + path, e);
                    }
                });
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BusinessException(500, "Failed to scan directory: " + cause.getMessage());
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to scan directory: " + e.getMessage());
        }
        return result;
    }

    private String computeMd5(Path filePath) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(filePath);
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) { }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    private String saveWithMd5(MultipartFile file, Path targetPath) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }

        try (InputStream is = file.getInputStream();
             DigestInputStream dis = new DigestInputStream(is, md)) {
            Files.copy(dis, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return HexFormat.of().formatHex(md.digest());
    }
}
