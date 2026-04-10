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
public class SkillController {

    @Resource
    private SkillService skillService;

    @Resource
    private FileStorageService fileStorageService;

    @PostMapping
    @RequireAuth
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
        skill.setTags(request.getTags());
        skill.setCategory(request.getCategory());
        skill.setIsPublic(request.getIsPublic());
        skill.setDownloadCount(0);
        skill.setLikeCount(0);
        Skill createdSkill = skillService.create(skill);
        return Result.success(createdSkill);
    }

    @PutMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "skill", idParam = "id")
    public Result<Skill> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        Skill updatedSkill = skillService.update(skill);
        return Result.success(updatedSkill);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequireOwnership(resourceType = "skill", idParam = "id")
    public Result<Void> deleteSkill(@PathVariable Long id) {
        skillService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Skill> getSkillById(@PathVariable Long id) {
        Skill skill = skillService.findById(id);
        return Result.success(skill);
    }

    @GetMapping
    public Result<List<Skill>> getAllSkills() {
        List<Skill> skills = skillService.findAll();
        return Result.success(skills);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Skill>> getSkillsByUserId(@PathVariable Long userId) {
        List<Skill> skills = skillService.findByUserId(userId);
        return Result.success(skills);
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<Skill>> getSkillsByAgentId(@PathVariable Long agentId) {
        List<Skill> skills = skillService.findByAgentId(agentId);
        return Result.success(skills);
    }

    @GetMapping("/category/{category}")
    public Result<List<Skill>> getSkillsByCategory(@PathVariable String category) {
        List<Skill> skills = skillService.findByCategory(category);
        return Result.success(skills);
    }

    @GetMapping("/public")
    public Result<List<Skill>> getPublicSkills() {
        List<Skill> skills = skillService.findByPublic(true);
        return Result.success(skills);
    }

    @GetMapping("/search")
    public Result<List<Skill>> searchSkills(@RequestParam String keyword) {
        List<Skill> skills = skillService.searchByKeyword(keyword);
        return Result.success(skills);
    }

    @PostMapping("/{id}/download")
    public Result<Void> incrementDownloadCount(@PathVariable Long id) {
        skillService.incrementDownloadCount(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    public Result<Void> incrementLikeCount(@PathVariable Long id) {
        skillService.incrementLikeCount(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @RequireAuth
    public Result<Integer> batchDeleteSkills(@RequestBody BatchDeleteRequest request) {
        int count = skillService.batchDelete(request.getIds());
        return Result.success(count);
    }

    @PostMapping("/{agentId}/upload")
    @RequireAuth
    @RequireOwnership(resourceType = "agent", idParam = "agentId")
    public Result<FileUploadResponse> uploadSkillFile(
            @PathVariable Long agentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        FileUploadResponse response = fileStorageService.saveFile(file, userId, agentId, "skill", description);
        return Result.success(response);
    }

    @GetMapping("/file/{fileId}")
    @RequireAuth
    public ResponseEntity<org.springframework.core.io.Resource> downloadSkillFile(@PathVariable Long fileId, HttpServletRequest httpRequest) {
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
    public Result<List<FileUploadLog>> getSkillFiles(@PathVariable Long agentId) {
        List<FileUploadLog> files = fileStorageService.getFileList(agentId, "skill", null);
        return Result.success(files);
    }

    @DeleteMapping("/file/{fileId}")
    @RequireAuth
    public Result<Void> deleteSkillFile(@PathVariable Long fileId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        fileStorageService.deleteFile(fileId, userId);
        return Result.success();
    }
}