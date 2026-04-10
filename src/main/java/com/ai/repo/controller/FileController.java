package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.mapper.FileUploadLogMapper;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Resource
    private FileUploadLogMapper fileUploadLogMapper;

    @GetMapping("/{fileType}/agent/{agentId}")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    public Result<List<FileUploadLog>> getFilesByAgent(
            @PathVariable String fileType,
            @PathVariable Long agentId) {
        
        if (!"skill".equals(fileType) && !"memory".equals(fileType)) {
            return Result.error(400, "Invalid file type. Must be 'skill' or 'memory'");
        }
        
        List<FileUploadLog> files = fileUploadLogMapper.selectByAgentIdAndFileType(agentId, fileType);
        return Result.success(files);
    }

    @GetMapping("/agent/{agentId}/stats")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    public Result<java.util.Map<String, Object>> getFileStats(@PathVariable Long agentId) {
        Long skillCount = fileUploadLogMapper.countByAgentId(agentId, "skill");
        Long memoryCount = fileUploadLogMapper.countByAgentId(agentId, "memory");
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("skillFileCount", skillCount);
        stats.put("memoryFileCount", memoryCount);
        stats.put("totalFileCount", skillCount + memoryCount);
        
        return Result.success(stats);
    }
}