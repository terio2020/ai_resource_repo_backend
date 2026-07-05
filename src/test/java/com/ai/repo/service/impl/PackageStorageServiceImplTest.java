package com.ai.repo.service.impl;

import com.ai.repo.entity.PackageFile;
import com.ai.repo.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PackageStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private PackageStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PackageStorageServiceImpl();
        ReflectionTestUtils.setField(service, "basePath", tempDir.toString());
        ReflectionTestUtils.setField(service, "allowedExtensions", ".md,.json,.txt,.py");
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 50L);
        ReflectionTestUtils.setField(service, "maxFilesPerVersion", 100);
        // Initialize the extension list (normally done by @PostConstruct)
        service.init();

        try {
            Files.createDirectories(tempDir);
        } catch (IOException ignored) {}
    }

    @Test
    void createVersionDirectory_shouldCreateDirs() {
        String dir = service.createVersionDirectory(
                tempDir.toString(), 1L, 10L, "skill", "test-pkg", "v1_20260622_120000");

        assertTrue(Files.exists(Path.of(dir)));
        assertTrue(dir.contains("skill/1/10/test-pkg/v1_20260622_120000"));
    }

    @Test
    void saveFiles_shouldSaveAndReturnMetadata() {
        String versionDir = service.createVersionDirectory(
                tempDir.toString(), 1L, 10L, "skill", "test-pkg", "v1");

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "hello.md", "text/markdown", "Hello World".getBytes()),
                new MockMultipartFile("files", "config.json", "application/json", "{\"key\": \"value\"}".getBytes())
        );

        List<PackageFile> result = service.saveFiles(1L, versionDir, files);

        assertEquals(2, result.size());
        assertTrue(Files.exists(Path.of(versionDir, "hello.md")));
        assertTrue(Files.exists(Path.of(versionDir, "config.json")));
        assertNotNull(result.get(0).getMd5Hash());
        assertEquals(32, result.get(0).getMd5Hash().length());
    }

    @Test
    void saveFiles_shouldRejectDisallowedExtension() {
        String versionDir = service.createVersionDirectory(
                tempDir.toString(), 1L, 10L, "skill", "test-pkg", "v1");

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "malware.exe", "application/x-msdownload", "bad".getBytes())
        );

        assertThrows(BusinessException.class, () -> service.saveFiles(1L, versionDir, files));
    }

    @Test
    void generateVersionTag_shouldReturnFormatted() {
        String tag = PackageStorageServiceImpl.generateVersionTag(1L, 5);
        assertTrue(tag.startsWith("v5_"));
        // format: v{num}_{yyyyMMdd_HHmmss} = 2 + 1 + 8 + 1 + 6 = 18 chars
        assertEquals(18, tag.length());
    }

@Test
void packAsZip_shouldCreateZip() throws IOException {
        String versionDir = service.createVersionDirectory(
                tempDir.toString(), 1L, 10L, "skill", "test-pkg", "v1");

        Files.writeString(Path.of(versionDir, "file1.md"), "content1");
        Files.writeString(Path.of(versionDir, "file2.json"), "{}");

        java.io.File zip = service.packAsZip(versionDir);
        assertTrue(zip.exists());
        assertTrue(zip.length() > 0);
    }

    // ==================== F3: path traversal regressions ====================

    @Test
    void createVersionDirectory_shouldRejectAbsolutePackageType() {
        assertThrows(BusinessException.class, () ->
                service.createVersionDirectory(tempDir.toString(), 1L, 10L, "/skill", "pkg", "v1"));
    }

    @Test
    void createVersionDirectory_shouldRejectParentInPackageName() {
        assertThrows(BusinessException.class, () ->
                service.createVersionDirectory(tempDir.toString(), 1L, 10L, "skill", "../escape", "v1"));
    }

    @Test
    void createVersionDirectory_shouldRejectSeparatorInVersionTag() {
        assertThrows(BusinessException.class, () ->
                service.createVersionDirectory(tempDir.toString(), 1L, 10L, "skill", "pkg", "v1/../../.."));
    }

    @Test
    void saveFiles_shouldRejectAbsoluteFileName_thatEscapesVersionDir() throws IOException {
        String versionDir = service.createVersionDirectory(
                tempDir.toString(), 1L, 10L, "skill", "test-pkg", "v1");

        // "/etc/passwd.txt" — Path.of(versionDir, "/etc/passwd.txt") would otherwise
        // resolve to the absolute path /etc/passwd.txt and overwrite it.
        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "/etc/passwd.txt", "text/plain", "pwned".getBytes())
        );

        assertThrows(BusinessException.class, () -> service.saveFiles(1L, versionDir, files));

        // Nothing should have escaped the version directory
        assertFalse(Files.exists(Path.of("/etc/passwd.txt")));
    }

    @Test
    void saveFiles_shouldRejectParentTraversalFileName() throws IOException {
        String versionDir = service.createVersionDirectory(
                tempDir.toString(), 1L, 10L, "skill", "test-pkg", "v1");

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "../escape.md", "text/markdown", "escape".getBytes())
        );

        assertThrows(BusinessException.class, () -> service.saveFiles(1L, versionDir, files));
        assertFalse(Files.exists(Path.of(tempDir.toString(), "escape.md")));
    }

    @Test
    void saveFiles_shouldRejectNullByteInFileName() throws IOException {
        String versionDir = service.createVersionDirectory(
                tempDir.toString(), 1L, 10L, "skill", "test-pkg", "v1");

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "fix\0.md", "text/markdown", "x".getBytes())
        );

        assertThrows(BusinessException.class, () -> service.saveFiles(1L, versionDir, files));
    }

    @Test
    void saveContributionFile_shouldRejectAbsoluteFileName() {
        assertThrows(BusinessException.class, () -> {
            MultipartFile file = new MockMultipartFile("files", "/etc/passwd.txt",
                    "text/plain", "x".getBytes());
            service.saveContributionFile(1L, tempDir.toString(), file);
        });
        assertFalse(Files.exists(Path.of("/etc/passwd.txt")));
    }

    @Test
    void saveContributionFile_shouldRejectParentTraversalFileName() {
        assertThrows(BusinessException.class, () -> {
            MultipartFile file = new MockMultipartFile("files", "../escape.md",
                    "text/markdown", "x".getBytes());
            service.saveContributionFile(1L, tempDir.toString(), file);
        });
    }
}
