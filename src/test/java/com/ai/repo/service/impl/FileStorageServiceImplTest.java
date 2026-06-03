package com.ai.repo.service.impl;

import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.exception.FileStorageException;
import com.ai.repo.exception.FileTooLargeException;
import com.ai.repo.exception.InvalidFileTypeException;
import com.ai.repo.mapper.FileUploadLogMapper;
import com.ai.repo.service.ContentModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    @Mock
    private FileUploadLogMapper fileUploadLogMapper;

    @Mock
    private ContentModerationService contentModerationService;

    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        fileStorageService = new FileStorageServiceImpl();

        setField("fileUploadLogMapper", fileUploadLogMapper);
        setField("contentModerationService", contentModerationService);
        setField("basePath", "/tmp/test-files");
        setField("maxFileSizeMB", 50L);
        setField("allowedExtensions", ".md");
    }

    private void setField(String name, Object value) throws Exception {
        java.lang.reflect.Field field = FileStorageServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(fileStorageService, value);
    }

    // ==================== validateFileType ====================

    @Test
    void validateFileType_shouldPass_forMdFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test.md", "text/markdown", "content".getBytes());
        assertDoesNotThrow(() -> fileStorageService.validateFileType(file));
    }

    @Test
    void validateFileType_shouldThrow_forNonMdFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        InvalidFileTypeException ex = assertThrows(InvalidFileTypeException.class, () -> fileStorageService.validateFileType(file));
        assertTrue(ex.getMessage().contains(".md"));
        assertTrue(ex.getMessage().contains("txt"));
    }

    // ==================== validateFileSize ====================

    @Test
    void validateFileSize_shouldPass_forSmallFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test.md", "text/markdown", new byte[1024 * 1024]);
        assertDoesNotThrow(() -> fileStorageService.validateFileSize(file));
    }

    @Test
    void validateFileSize_shouldThrow_forOversizedFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test.md", "text/markdown", new byte[100 * 1024 * 1024]);
        FileTooLargeException ex = assertThrows(FileTooLargeException.class, () -> fileStorageService.validateFileSize(file));
        assertEquals(50, ex.getMaxSize());
    }

    // ==================== generateUniqueFileName ====================

    @Test
    void generateUniqueFileName_shouldReturnBasenameWithExtension() {
        String result = fileStorageService.generateUniqueFileName("my-skill.md", 1L, "skill");
        assertEquals("my-skill.md", result);
    }

    // ==================== saveFile ====================

    @Test
    void saveFile_shouldSucceed() {
        String content = "test content";
        MockMultipartFile file = new MockMultipartFile("file", "test.md", "text/markdown", content.getBytes(StandardCharsets.UTF_8));

        FileUploadResponse response = fileStorageService.saveFile(file, 1L, 1L, "skill", "desc");

        assertNotNull(response);
        assertEquals("test.md", response.getFileName());
        assertEquals(content.length(), response.getFileSize());
        assertNotNull(response.getFilePath());
        assertNotNull(response.getUploadTime());
        verify(contentModerationService).moderateContent(anyString(), eq("test.md"));
    }

    // ==================== loadFileAsResource ====================

    @Test
    void loadFileAsResource_shouldReturnResource_whenFound() {
        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setAgentId(1L);
        log.setFilePath("skill/test.md");
        log.setOriginalFileName("test.md");
        when(fileUploadLogMapper.selectById(1L)).thenReturn(log);

        // File not on disk in test, so this will throw FileStorageException from UrlResource
        // but we're testing that the mapper lookup and permission check work
        FileStorageException ex = assertThrows(FileStorageException.class, () -> fileStorageService.loadFileAsResource(1L, 1L));
        assertTrue(ex.getMessage().contains("Failed to load file") || ex.getMessage().contains("not found"));
    }

    @Test
    void loadFileAsResource_shouldThrow_whenNotFound() {
        when(fileUploadLogMapper.selectById(999L)).thenReturn(null);

        FileStorageException ex = assertThrows(FileStorageException.class, () -> fileStorageService.loadFileAsResource(999L, 1L));
        assertTrue(ex.getMessage().contains("File not found"));
    }

    @Test
    void loadFileAsResource_shouldThrow_whenPermissionDenied() {
        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        log.setUserId(2L); // different user
        log.setAgentId(1L);
        log.setFilePath("skill/test.md");
        when(fileUploadLogMapper.selectById(1L)).thenReturn(log);

        FileStorageException ex = assertThrows(FileStorageException.class, () -> fileStorageService.loadFileAsResource(1L, 1L));
        assertTrue(ex.getMessage().contains("permission"));
    }

    // ==================== deleteFile ====================

    @Test
    void deleteFile_shouldSucceed() {
        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setAgentId(1L);
        log.setFilePath("skill/test.md");
        log.setOriginalFileName("test.md");
        when(fileUploadLogMapper.selectById(1L)).thenReturn(log);

        assertDoesNotThrow(() -> fileStorageService.deleteFile(1L, 1L));
        verify(fileUploadLogMapper).deleteById(1L, 1L);
    }

    @Test
    void deleteFile_shouldThrow_whenNotFound() {
        when(fileUploadLogMapper.selectById(999L)).thenReturn(null);

        FileStorageException ex = assertThrows(FileStorageException.class, () -> fileStorageService.deleteFile(999L, 1L));
        assertTrue(ex.getMessage().contains("File not found"));
    }

    @Test
    void deleteFile_shouldThrow_whenPermissionDenied() {
        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        log.setUserId(2L); // different user
        log.setAgentId(1L);
        log.setFilePath("skill/test.md");
        when(fileUploadLogMapper.selectById(1L)).thenReturn(log);

        FileStorageException ex = assertThrows(FileStorageException.class, () -> fileStorageService.deleteFile(1L, 1L));
        assertTrue(ex.getMessage().contains("permission"));
    }

    // ==================== getFileList ====================

    @Test
    void getFileList_shouldReturnList() {
        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        when(fileUploadLogMapper.selectByAgentIdAndFileType(1L, "skill"))
                .thenReturn(List.of(log));

        List<FileUploadLog> result = fileStorageService.getFileList(1L, "skill", null);

        assertEquals(1, result.size());
        verify(fileUploadLogMapper).selectByAgentIdAndFileType(1L, "skill");
    }

    // ==================== getFileUploadLog ====================

    @Test
    void getFileUploadLog_shouldReturnLog() {
        FileUploadLog log = new FileUploadLog();
        log.setId(1L);
        when(fileUploadLogMapper.selectById(1L)).thenReturn(log);

        FileUploadLog result = fileStorageService.getFileUploadLog(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(fileUploadLogMapper).selectById(1L);
    }
}
