package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.BatchDeleteRequest;
import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.dto.SkillCreateRequest;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.entity.Skill;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.FileStorageService;
import com.ai.repo.service.SkillService;
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
@RequestMapping("/api/skills")
@Tag(name = "Skill API", description = "Skill management operations")
public class SkillController {

    @Resource
    private SkillService skillService;

    @Resource
    private FileStorageService fileStorageService;

    @PostMapping
    @RequireAuth
    @Operation(summary = "Create a new skill", description = "Create a new skill with provided details")
    public Result<Skill> createSkill(@RequestBody SkillCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Skill skill = new Skill();
        skill.setUserId(userId);
        skill.setAgentId(request.getAgentId());
        skill.setName(request.getName());
        skill.setVersion(request.getVersion());
        skill.setDescription(request.getDescription());
        skill.setFilePath(request.getFilePath());
        skill.setFileSize(request.getFileSize());
        skill.setMimeType(request.getMimeType());

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            skill.setTags(String.join(",", request.getTags()));
        }

        skill.setCategory(request.getCategory());
        skill.setIsPublic(request.getIsPublic());
        skill.setDownloadCount(0);
        skill.setLikeCount(0);
        Skill createdSkill = skillService.upsert(skill);
        return Result.success(createdSkill);
    }

    @PutMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "skill", idParam = "id")
    @Operation(summary = "Update a skill", description = "Update an existing skill with new information")
    public Result<Skill> updateSkill(
            @Parameter(description = "Skill ID") @PathVariable Long id,
            @RequestBody Skill skill) {
        skill.setId(id);
        Skill updatedSkill = skillService.update(skill);
        return Result.success(updatedSkill);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "skill", idParam = "id")
    @Operation(summary = "Delete a skill", description = "Delete a skill by its ID")
    public Result<Void> deleteSkill(@Parameter(description = "Skill ID") @PathVariable Long id) {
        skillService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get skill by ID", description = "Retrieve a specific skill by its ID")
    public Result<Skill> getSkillById(@Parameter(description = "Skill ID") @PathVariable Long id) {
        Skill skill = skillService.findById(id);
        return Result.success(skill);
    }

    @GetMapping
    @Operation(summary = "Get all skills", description = "Retrieve all available skills")
    public Result<List<Skill>> getAllSkills() {
        List<Skill> skills = skillService.findAll();
        return Result.success(skills);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get skills by user", description = "Retrieve all skills owned by a specific user")
    public Result<List<Skill>> getSkillsByUserId(@Parameter(description = "User ID") @PathVariable Long userId) {
        List<Skill> skills = skillService.findByUserId(userId);
        return Result.success(skills);
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Get skills by agent", description = "Retrieve all skills belonging to a specific agent")
    public Result<List<Skill>> getSkillsByAgentId(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        List<Skill> skills = skillService.findByAgentId(agentId);
        return Result.success(skills);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get skills by category", description = "Retrieve all skills in a specific category")
    public Result<List<Skill>> getSkillsByCategory(@Parameter(description = "Category name") @PathVariable String category) {
        List<Skill> skills = skillService.findByCategory(category);
        return Result.success(skills);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public skills", description = "Retrieve all public skills")
    public Result<List<Skill>> getPublicSkills() {
        List<Skill> skills = skillService.findByPublic(true);
        return Result.success(skills);
    }

    @GetMapping("/search")
    @Operation(summary = "Search skills", description = "Search skills by keyword")
    public Result<List<Skill>> searchSkills(@Parameter(description = "Search keyword") @RequestParam String keyword) {
        List<Skill> skills = skillService.searchByKeyword(keyword);
        return Result.success(skills);
    }

    @PostMapping("/{id}/download")
    @Operation(summary = "Increment download count", description = "Increment the download count of a skill")
    public Result<Void> incrementDownloadCount(@Parameter(description = "Skill ID") @PathVariable Long id) {
        skillService.incrementDownloadCount(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Increment like like", description = "Increment the like count of a skill")
    public Result<Void> incrementLikeCount(@Parameter(description = "Skill ID") @PathVariable Long id) {
        skillService.incrementLikeCount(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @RequireAuth
    @Operation(summary = "Batch delete skills", description = "Delete multiple skills at once")
    public Result<Integer> batchDeleteSkills(@RequestBody BatchDeleteRequest request) {
        int count = skillService.batchDelete(request.getIds());
        return Result.success(count);
    }

    @PostMapping("/{agentId}/upload")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    @Operation(summary = "Upload skill file", description = "Upload a file associated with a skill")
    public Result<FileUploadResponse> uploadSkillFile(
            @Parameter(description = "Agent ID") @PathVariable Long agentId,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "File description") @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        FileUploadResponse response = fileStorageService.saveFile(file, userId, agentId, "skill", description);
        return Result.success(response);
    }

    @GetMapping("/file/{fileId}")
    @RequireAuth
    @Operation(summary = "Download skill file", description = "Download a skill file by its file ID")
    public ResponseEntity<org.springframework.core.io.Resource> downloadSkillFile(
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
    @Operation(summary = "Get skill files", description = "Retrieve all skill files for an agent")
    public Result<List<FileUploadLog>> getSkillFiles(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        List<FileUploadLog> files = fileStorageService.getFileList(agentId, "skill", null);
        return Result.success(files);
    }

    @DeleteMapping("/file/{fileId}")
    @RequireAuth
    @Operation(summary = "Delete skill file", description = "Delete a skill file by its ID")
    public Result<Void> deleteSkillFile(
            @Parameter(description = "File ID") @PathVariable Long fileId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        fileStorageService.deleteFile(fileId, userId);
        return Result.success();
    }
}
