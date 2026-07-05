package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.mapper.FileUploadLogMapper;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NOTE: This controller provides unified READ-ONLY query endpoints for file metadata.
 * <p>
 * File UPLOAD is intentionally NOT implemented here. Uploads are handled per-resource in
 * {@link MemoryController} (POST /api/memories/{agentId}/upload).
 * Do NOT add a POST /api/files/upload endpoint here.
 */
@RestController
@RequestMapping("/api/files")
@Validated
@Tag(name = "File API", description = "File management operations")
public class FileController {

    @Resource
    private FileUploadLogMapper fileUploadLogMapper;

    @GetMapping("/{fileType}/agent/{agentId}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    @Operation(summary = "Get files by agent and type", description = "Retrieve files for an agent by file type")
    public ResponseEntity<Result<List<FileUploadLog>>> getFilesByAgent(
            @Parameter(description = "File type (skill or memory)") @PathVariable String fileType,
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {

        if (!"memory".equals(fileType)) {
            return Result.fail(400, "Invalid file type. Must be 'memory'");
        }

        List<FileUploadLog> files = fileUploadLogMapper.selectByAgentIdAndFileType(agentId, fileType);
        return Result.ok(files);
    }

    @GetMapping("/agent/{agentId}/stats")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    @Operation(summary = "Get file statistics", description = "Get file statistics for an agent")
    public ResponseEntity<Result<java.util.Map<String, Object>>> getFileStats(@Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {
        Long memoryCount = fileUploadLogMapper.countByAgentId(agentId, "memory");

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("memoryFileCount", memoryCount);
        stats.put("totalFileCount", memoryCount);

        return Result.ok(stats);
    }
}
