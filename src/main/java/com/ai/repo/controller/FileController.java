package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.mapper.FileUploadLogMapper;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File API", description = "File management operations")
public class FileController {

    @Resource
    private FileUploadLogMapper fileUploadLogMapper;

    @GetMapping("/{fileType}/agent/{agentId}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    @Operation(summary = "Get files by agent and type", description = "Retrieve files for an agent by file type")
    public Result<List<FileUploadLog>> getFilesByAgent(
            @Parameter(description = "File type (skill or memory)") @PathVariable String fileType,
            @Parameter(description = "Agent ID") @PathVariable Long agentId) {

        if (!"skill".equals(fileType) && !"memory".equals(fileType)) {
            return Result.error(400, "Invalid file type. Must be 'skill' or 'memory'");
        }

        List<FileUploadLog> files = fileUploadLogMapper.selectByAgentIdAndFileType(agentId, fileType);
        return Result.success(files);
    }

    @GetMapping("/agent/{agentId}/stats")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    @Operation(summary = "Get file statistics", description = "Get file statistics for an agent")
    public Result<java.util.Map<String, Object>> getFileStats(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        Long skillCount = fileUploadLogMapper.countByAgentId(agentId, "skill");
        Long memoryCount = fileUploadLogMapper.countByAgentId(agentId, "memory");

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("skillFileCount", skillCount);
        stats.put("memoryFileCount", memoryCount);
        stats.put("totalFileCount", skillCount + memoryCount);

        return Result.success(stats);
    }
}
