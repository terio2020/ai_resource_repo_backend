package com.ai.repo.controller;

import com.ai.repo.aspect.RateLimit;
import com.ai.repo.common.Result;
import com.ai.repo.dto.BatchDeleteRequest;
import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.dto.MemoryCreateRequest;
import com.ai.repo.dto.MemoryUpdateRequest;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.entity.Memory;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.FileStorageService;
import com.ai.repo.service.MemoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
@Validated
@Tag(name = "Memory API", description = "Memory management operations")
public class MemoryController {

    @Resource
    private MemoryService memoryService;

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private ObjectMapper objectMapper;

    @PostMapping
    @ApiKeyAuth
    @Operation(summary = "Create or update a memory", description = "Create or update a memory with provided details")
    public ResponseEntity<Result<Memory>> createMemory(@RequestBody MemoryCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        // When using JWT auth, agentId comes from request body; when using API key auth, it comes from interceptor
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        if (agentId == null) {
            agentId = request.getAgentId();
        }

        // agentId is required for memory creation — validate early
        if (agentId == null) {
            throw new com.ai.repo.exception.BusinessException(400, "Agent ID is required for memory creation");
        }

        // Default title if not provided
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = "Memory_" + System.currentTimeMillis();
        }

        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setAgentId(agentId);
        memory.setTitle(title);
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
        memory.setMetadata(serializeMetadata(request.getMetadata()));
        memory.setDownloadCount(0);
        memory.setLikeCount(0);

        Memory createdMemory = memoryService.upsert(memory);
        return Result.ok(createdMemory);
    }

    private String serializeMetadata(Object metadata) {
        if (metadata == null) return null;
        if (metadata instanceof String) return (String) metadata;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return metadata.toString();
        }
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update a memory", description = "Update an existing memory with new information")
    public ResponseEntity<Result<Memory>> updateMemory(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody MemoryUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        if (agentId == null) {
            agentId = request.getAgentId();
        }
        Memory memory = new Memory();
        memory.setId(id);
        memory.setUserId(userId);
        memory.setAgentId(agentId);
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
        memory.setMetadata(serializeMetadata(request.getMetadata()));
        Memory updatedMemory = memoryService.update(memory);
        return Result.ok(updatedMemory);
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete a memory", description = "Delete a memory by its ID")
    public ResponseEntity<Result<Void>> deleteMemory(@PathVariable @Min(1) Long id) {
        memoryService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Get memory by ID", description = "Retrieve a specific memory by its ID")
    public ResponseEntity<Result<Memory>> getMemoryById(@PathVariable @Min(1) Long id) {
        Memory memory = memoryService.findById(id);
        return Result.ok(memory);
    }

    @GetMapping("/user/{userId}")
    @RequireAuth
    @Operation(summary = "Get memories by user", description = "Retrieve all memories owned by a specific user")
    public ResponseEntity<Result<List<Memory>>> getMemoriesByUserId(@PathVariable @Min(1) Long userId) {
        List<Memory> memories = memoryService.findByUserId(userId);
        return Result.ok(memories);
    }

    @GetMapping("/agent/{agentId}")
    @RequireAuth
    @Operation(summary = "Get memories by agent", description = "Retrieve all memories belonging to a specific agent")
    public ResponseEntity<Result<List<Memory>>> getMemoriesByAgentId(@PathVariable @Min(1) Long agentId) {
        List<Memory> memories = memoryService.findByAgentId(agentId);
        return Result.ok(memories);
    }

    @GetMapping("/category/{category}")
    @RequireAuth
    @Operation(summary = "Get memories by category", description = "Retrieve all memories in a specific category")
    public ResponseEntity<Result<List<Memory>>> getMemoriesByCategory(@PathVariable String category) {
        List<Memory> memories = memoryService.findByCategory(category);
        return Result.ok(memories);
    }

    @GetMapping("/public")
    @RequireAuth
    @Operation(summary = "Get public memories", description = "Retrieve all public memories")
    public ResponseEntity<Result<List<Memory>>> getPublicMemories() {
        List<Memory> memories = memoryService.findByPublic(true);
        return Result.ok(memories);
    }

    @GetMapping("/search")
    @RequireAuth
    @Operation(summary = "Search memories", description = "Search memories by keyword")
    public ResponseEntity<Result<List<Memory>>> searchMemories(@RequestParam String keyword) {
        List<Memory> memories = memoryService.searchByKeyword(keyword);
        return Result.ok(memories);
    }

    @DeleteMapping("/batch")
    @ApiKeyAuth
    @Operation(summary = "Batch delete memories", description = "Delete multiple memories at once")
    public ResponseEntity<Result<Integer>> batchDeleteMemories(@RequestBody BatchDeleteRequest request) {
        int count = memoryService.batchDelete(request.getIds());
        return Result.ok(count);
    }

    @PostMapping("/{id}/download")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment download count", description = "Increment download count of a memory")
    public ResponseEntity<Result<Void>> incrementDownloadCount(@PathVariable @Min(1) Long id) {
        memoryService.incrementDownloadCount(id);
        return Result.ok();
    }

    @PostMapping("/{id}/like")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment like count", description = "Increment the like count of a memory")
    public ResponseEntity<Result<Void>> incrementLikeCount(@PathVariable @Min(1) Long id) {
        memoryService.incrementLikeCount(id);
        return Result.ok();
    }

    @PostMapping("/{agentId}/upload")
    @ApiKeyAuth
    @Operation(summary = "Upload memory file", description = "Upload a file associated with a memory")
    public ResponseEntity<Result<FileUploadResponse>> uploadMemoryFile(
            @PathVariable @Min(1) Long agentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        FileUploadResponse response = fileStorageService.saveFile(file, userId, agentId, "memory", description);
        return Result.ok(response);
    }

    @GetMapping("/file/{fileId}")
    @ApiKeyAuth
    @Operation(summary = "Download memory file", description = "Download a memory file by its file ID")
    public ResponseEntity<org.springframework.core.io.Resource> downloadMemoryFile(
            @PathVariable @Min(1) Long fileId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(fileId, userId);
        FileUploadLog uploadLog = fileStorageService.getFileUploadLog(fileId);

        String contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uploadLog.getOriginalFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{agentId}/files")
    @ApiKeyAuth
    @Operation(summary = "Get memory files", description = "Retrieve all memory files for an agent")
    public ResponseEntity<Result<List<FileUploadLog>>> getMemoryFiles(@PathVariable @Min(1) Long agentId) {
        List<FileUploadLog> files = fileStorageService.getFileList(agentId, "memory", null);
        return Result.ok(files);
    }

    @DeleteMapping("/file/{fileId}")
    @ApiKeyAuth
    @Operation(summary = "Delete memory file", description = "Delete a memory file by its file ID")
    public ResponseEntity<Result<Void>> deleteMemoryFile(
            @PathVariable @Min(1) Long fileId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        fileStorageService.deleteFile(fileId, userId);
        return Result.ok();
    }
}
