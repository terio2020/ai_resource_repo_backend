package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.BatchDeleteRequest;
import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.entity.Memory;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.FileStorageService;
import com.ai.repo.service.MemoryService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    @Resource
    private MemoryService memoryService;

    @Resource
    private FileStorageService fileStorageService;

    @PostMapping
    @RequireAuth
    public Result<Memory> createMemory(@RequestBody Memory memory, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        memory.setUserId(userId);
        Memory createdMemory = memoryService.create(memory);
        return Result.success(createdMemory);
    }

    @PutMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "memory", idParam = "id")
    public Result<Memory> updateMemory(@PathVariable Long id, @RequestBody Memory memory) {
        memory.setId(id);
        Memory updatedMemory = memoryService.update(memory);
        return Result.success(updatedMemory);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "memory", idParam = "id")
    public Result<Void> deleteMemory(@PathVariable Long id) {
        memoryService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Memory> getMemoryById(@PathVariable Long id) {
        Memory memory = memoryService.findById(id);
        return Result.success(memory);
    }

    @GetMapping
    public Result<List<Memory>> getAllMemories() {
        List<Memory> memories = memoryService.findAll();
        return Result.success(memories);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Memory>> getMemoriesByUserId(@PathVariable Long userId) {
        List<Memory> memories = memoryService.findByUserId(userId);
        return Result.success(memories);
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<Memory>> getMemoriesByAgentId(@PathVariable Long agentId) {
        List<Memory> memories = memoryService.findByAgentId(agentId);
        return Result.success(memories);
    }

    @GetMapping("/category/{category}")
    public Result<List<Memory>> getMemoriesByCategory(@PathVariable String category) {
        List<Memory> memories = memoryService.findByCategory(category);
        return Result.success(memories);
    }

    @GetMapping("/search")
    public Result<List<Memory>> searchMemories(@RequestParam String keyword) {
        List<Memory> memories = memoryService.searchByKeyword(keyword);
        return Result.success(memories);
    }

    @DeleteMapping("/batch")
    @RequireAuth
    public Result<Integer> batchDeleteMemories(@RequestBody BatchDeleteRequest request) {
        int count = memoryService.batchDelete(request.getIds());
        return Result.success(count);
    }

    @PostMapping("/{agentId}/upload")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    public Result<FileUploadResponse> uploadMemoryFile(
            @PathVariable Long agentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        FileUploadResponse response = fileStorageService.saveFile(file, userId, agentId, "memory", description);
        return Result.success(response);
    }

    @GetMapping("/file/{fileId}")
    @RequireAuth
    public ResponseEntity<org.springframework.core.io.Resource> downloadMemoryFile(@PathVariable Long fileId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(fileId, userId);
        FileUploadLog uploadLog = fileStorageService.getFileUploadLog(fileId);

        String contentType = null;
        try {
            contentType = "application/octet-stream";
        } catch (Exception ex) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uploadLog.getOriginalFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{agentId}/files")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    public Result<List<FileUploadLog>> getMemoryFiles(@PathVariable Long agentId) {
        List<FileUploadLog> files = fileStorageService.getFileList(agentId, "memory", null);
        return Result.success(files);
    }

    @DeleteMapping("/file/{fileId}")
    @RequireAuth
    public Result<Void> deleteMemoryFile(@PathVariable Long fileId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        fileStorageService.deleteFile(fileId, userId);
        return Result.success();
    }
}
