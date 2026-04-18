package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.BatchDeleteRequest;
import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.dto.MemoryCreateRequest;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.entity.Memory;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.FileStorageService;
import com.ai.repo.service.MemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Memory API", description = "Memory management operations")
public class MemoryController {

    @Resource
    private MemoryService memoryService;

    @Resource
    private FileStorageService fileStorageService;

    @PostMapping
    @RequireAuth
    @Operation(summary = "Create or update a memory", description = "Create or update a memory with provided details")
    public Result<Memory> createMemory(@RequestBody MemoryCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setAgentId(request.getAgentId());
        memory.setTitle(request.getTitle());
        memory.setContent(request.getContent());
        memory.setVersion(request.getVersion());
        memory.setDescription(request.getDescription());
        memory.setFilePath(request.getFilePath());
        memory.setFileSize(request.getFileSize());
        memory.setMimeType(request.getMimeType());

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            memory.setTags(String.join(",", request.getTags()));
        }

        memory.setCategory(request.getCategory());
        memory.setIsPublic(request.getIsPublic());
        memory.setMetadata(request.getMetadata());

        Memory createdMemory = memoryService.upsert(memory);
        return Result.success(createdMemory);
    }

    @PutMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "memory", idParam = "id")
    @Operation(summary = "Update a memory", description = "Update an existing memory with new information")
    public Result<Memory> updateMemory(
            @Parameter(description = "Memory ID") @PathVariable Long id,
            @RequestBody Memory memory) {
        memory.setId(id);
        Memory updatedMemory = memoryService.update(memory);
        return Result.success(updatedMemory);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "memory", idParam = "id")
    @Operation(summary = "Delete a memory", description = "Delete a memory by its ID")
    public Result<Void> deleteMemory(@Parameter(description = "Memory ID") @PathVariable Long id) {
        memoryService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get memory by ID", description = "Retrieve a specific memory by its ID")
    public Result<Memory> getMemoryById(@Parameter(description = "Memory ID") @PathVariable Long id) {
        Memory memory = memoryService.findById(id);
        return Result.success(memory);
    }

    @GetMapping
    @Operation(summary = "Get all memories", description = "Retrieve all available memories")
    public Result<List<Memory>> getAllMemories() {
        List<Memory> memories = memoryService.findAll();
        return Result.success(memories);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get memories by user", description = "Retrieve all memories owned by a specific user")
    public Result<List<Memory>> getMemoriesByUserId(@Parameter(description = "User ID") @PathVariable Long userId) {
        List<Memory> memories = memoryService.findByUserId(userId);
        return Result.success(memories);
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Get memories by agent", description = "Retrieve all memories belonging to a specific agent")
    public Result<List<Memory>> getMemoriesByAgentId(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        List<Memory> memories = memoryService.findByAgentId(agentId);
        return Result.success(memories);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get memories by category", description = "Retrieve all memories in a specific category")
    public Result<List<Memory>> getMemoriesByCategory(@Parameter(description = "Category name") @PathVariable String category) {
        List<Memory> memories = memoryService.findByCategory(category);
        return Result.success(memories);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public memories", description = "Retrieve all public memories")
    public Result<List<Memory>> getPublicMemories() {
        List<Memory> memories = memoryService.findByPublic(true);
        return Result.success(memories);
    }

    @GetMapping("/search")
    @Operation(summary = "Search memories", description = "Search memories by keyword")
    public Result<List<Memory>> searchMemories(@Parameter(description = "Search keyword") @RequestParam String keyword) {
        List<Memory> memories = memoryService.searchByKeyword(keyword);
        return Result.success(memories);
    }

    @DeleteMapping("/batch")
    @RequireAuth
    @Operation(summary = "Batch delete memories", description = "Delete multiple memories at once")
    public Result<Integer> batchDeleteMemories(@RequestBody BatchDeleteRequest request) {
        int count = memoryService.batchDelete(request.getIds());
        return Result.success(count);
    }

    @PostMapping("/{id}/download")
    @Operation(summary = "Increment download count", description = "Increment download count of a memory")
    public Result<Void> incrementDownloadCount(@Parameter(description = "Memory ID") @PathVariable Long id) {
        memoryService.incrementDownloadCount(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Increment like count", description = "Increment like count of a memory")
    public Result<Void> incrementLikeCount(@Parameter(description = "Memory ID") @PathVariable Long id) {
        memoryService.incrementLikeCount(id);
        return Result.success();
    }

    @PostMapping("/{agentId}/upload")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    @Operation(summary = "Upload memory file", description = "Upload a file associated with a memory")
    public Result<FileUploadResponse> uploadMemoryFile(
            @Parameter(description = "Agent ID") @PathVariable Long agentId,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "File description") @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        FileUploadResponse response = fileStorageService.saveFile(file, userId, agentId, "memory", description);
        return Result.success(response);
    }

    @GetMapping("/file/{fileId}")
    @RequireAuth
    @Operation(summary = "Download memory file", description = "Download a memory file by its file ID")
    public ResponseEntity<org.springframework.core.io.Resource> downloadMemoryFile(
            @Parameter(description = "File ID") @PathVariable Long fileId,
            HttpServletRequest httpRequest) {
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
    @Operation(summary = "Get memory files", description = "Retrieve all memory files for an agent")
    public Result<List<FileUploadLog>> getMemoryFiles(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        List<FileUploadLog> files = fileStorageService.getFileList(agentId, "memory", null);
        return Result.success(files);
    }

    @DeleteMapping("/file/{fileId}")
    @RequireAuth
    @Operation(summary = "Delete memory file", description = "Delete a memory file by its ID")
    public Result<Void> deleteMemoryFile(
            @Parameter(description = "File ID") @PathVariable Long fileId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        fileStorageService.deleteFile(fileId, userId);
        return Result.success();
    }
}
