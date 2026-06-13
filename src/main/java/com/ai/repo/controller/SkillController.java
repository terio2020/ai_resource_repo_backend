package com.ai.repo.controller;

import com.ai.repo.aspect.RateLimit;
import com.ai.repo.common.PageResult;
import com.ai.repo.common.Result;
import com.ai.repo.dto.BatchDeleteRequest;
import com.ai.repo.dto.FileUploadResponse;
import com.ai.repo.dto.SkillCreateRequest;
import com.ai.repo.dto.SkillUpdateRequest;
import com.ai.repo.entity.FileUploadLog;
import com.ai.repo.entity.Skill;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.security.RequireOwnership;
import com.ai.repo.service.FileStorageService;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.ShareService;
import com.ai.repo.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/skills")
@Validated
@Tag(name = "Skill API", description = "Skill management operations")
public class SkillController {

    @Resource
    private SkillService skillService;

    @Resource
    private AgentService agentService;

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private ShareService shareService;

    @PostMapping
    @ApiKeyAuth
    @Operation(summary = "Create a new skill", description = "Create a new skill with provided details")
    public Result<Skill> createSkill(@RequestBody SkillCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Skill skill = new Skill();
        skill.setUserId(userId);
        skill.setAgentId(agentId);
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
        skill.setType(request.getType());
        skill.setEnabled(request.getEnabled());
        skill.setIsPublic(request.getIsPublic());
        skill.setDownloadCount(0);
        skill.setLikeCount(0);
        Skill createdSkill = skillService.upsert(skill);
        return Result.success(createdSkill);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update a skill", description = "Update an existing skill with new information")
    public Result<Skill> updateSkill(
            @PathVariable @Min(1) Long id,
            @RequestBody SkillUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Skill skill = new Skill();
        skill.setId(id);
        skill.setUserId(userId);
        skill.setAgentId(agentId);
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
        skill.setType(request.getType());
        skill.setEnabled(request.getEnabled());
        skill.setIsPublic(request.getIsPublic());
        Skill updatedSkill = skillService.update(skill);
        return Result.success(updatedSkill);
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete a skill", description = "Delete a skill by its ID")
    public Result<Void> deleteSkill(@PathVariable @Min(1) Long id) {
        skillService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Get skill by ID", description = "Retrieve a specific skill by its ID")
    public Result<Skill> getSkillById(@PathVariable @Min(1) Long id) {
        Skill skill = skillService.findById(id);
        return Result.success(skill);
    }

    @GetMapping
    @RequireAuth
    @Operation(summary = "List skills with pagination", description = "Retrieve paginated list of skills")
    public Result<PageResult<Skill>> listSkills(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Skill> pageResult = skillService.findAllPaginated(page, pageSize);
        return Result.success(pageResult);
    }

    @GetMapping("/user/{userId}")
    @RequireAuth
    @Operation(summary = "Get skills by user", description = "Retrieve all skills owned by a specific user")
    public Result<List<Skill>> getSkillsByUserId(@PathVariable @Min(1) Long userId) {
        List<Skill> skills = skillService.findByUserId(userId);
        return Result.success(skills);
    }

    @GetMapping("/agent/{agentId}")
    @RequireAuth
    @Operation(summary = "Get skills by agent", description = "Retrieve all skills belonging to a specific agent (FK + association table)")
    public Result<List<Skill>> getSkillsByAgentId(@PathVariable @Min(1) Long agentId) {
        List<Skill> directSkills = skillService.findByAgentId(agentId);
        List<Skill> boundSkills = agentService.getAgentSkills(agentId);
        Set<Long> seen = new HashSet<>();
        List<Skill> merged = new ArrayList<>();
        for (Skill s : directSkills) {
            if (seen.add(s.getId())) merged.add(s);
        }
        for (Skill s : boundSkills) {
            if (seen.add(s.getId())) merged.add(s);
        }
        return Result.success(merged);
    }

    @GetMapping("/category/{category}")
    @RequireAuth
    @Operation(summary = "Get skills by category", description = "Retrieve all skills in a specific category")
    public Result<List<Skill>> getSkillsByCategory(@PathVariable String category) {
        List<Skill> skills = skillService.findByCategory(category);
        return Result.success(skills);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public skills", description = "Retrieve all public skills. No authentication required.")
    public Result<List<Skill>> getPublicSkills() {
        List<Skill> skills = skillService.findByPublic(true);
        return Result.success(skills);
    }

    @GetMapping("/search")
    @RequireAuth
    @Operation(summary = "Search skills", description = "Search skills by keyword")
    public Result<List<Skill>> searchSkills(@RequestParam("q") String keyword) {
        List<Skill> skills = skillService.searchByKeyword(keyword);
        return Result.success(skills);
    }

    @PostMapping("/{id}/download")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment download count", description = "Increment the download count of a skill. Agent-only.")
    public Result<Void> incrementDownloadCount(@PathVariable @Min(1) Long id, HttpServletRequest httpRequest) {
        requireAgent(httpRequest);
        skillService.incrementDownloadCount(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    @ApiKeyAuth
    @RateLimit(value = 10, period = 60)
    @Operation(summary = "Increment like count", description = "Increment the like count of a skill. Agent-only.")
    public Result<Void> incrementLikeCount(@PathVariable @Min(1) Long id, HttpServletRequest httpRequest) {
        requireAgent(httpRequest);
        skillService.incrementLikeCount(id);
        return Result.success();
    }

    private void requireAgent(HttpServletRequest request) {
        if (request.getAttribute("agentId") == null) {
            throw new BusinessException(403, "Only agents can perform this action");
        }
    }

    @PostMapping("/{id}/share")
    @RequireAuth
    @Operation(summary = "Share a skill", description = "Generate a share link for a public skill. Human users only.")
    public Result<Map<String, String>> shareSkill(
            @Parameter(description = "Skill ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String token = shareService.createShareLink(id, userId);
        String shareUrl = "/api/skills/shared/" + token;
        return Result.success(Map.of("shareUrl", shareUrl, "shareToken", token));
    }

    @GetMapping("/shared/{token}")
    @Operation(summary = "View shared skill", description = "Access a shared skill via its share token. No authentication required.")
    public Result<Skill> getSharedSkill(
            @Parameter(description = "Share token") @PathVariable String token) {
        Skill skill = shareService.getSharedSkill(token);
        return Result.success(skill);
    }

    @DeleteMapping("/batch")
    @ApiKeyAuth
    @Operation(summary = "Batch delete skills", description = "Delete multiple skills at once")
    public Result<Integer> batchDeleteSkills(@RequestBody BatchDeleteRequest request) {
        int count = skillService.batchDelete(request.getIds());
        return Result.success(count);
    }

    @PostMapping("/{agentId}/upload")
    @ApiKeyAuth
    @Operation(summary = "Upload skill file", description = "Upload a file associated with a skill")
    public Result<FileUploadResponse> uploadSkillFile(
            @PathVariable @Min(1) Long agentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        FileUploadResponse response = fileStorageService.saveFile(file, userId, agentId, "skill", description);
        return Result.success(response);
    }

    @GetMapping("/file/{fileId}")
    @ApiKeyAuth
    @Operation(summary = "Download skill file", description = "Download a skill file by its file ID")
    public ResponseEntity<org.springframework.core.io.Resource> downloadSkillFile(
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
    @Operation(summary = "Get skill files", description = "Retrieve all skill files for an agent")
    public Result<List<FileUploadLog>> getSkillFiles(@PathVariable @Min(1) Long agentId) {
        List<FileUploadLog> files = fileStorageService.getFileList(agentId, "skill", null);
        return Result.success(files);
    }

    @DeleteMapping("/file/{fileId}")
    @ApiKeyAuth
    @Operation(summary = "Delete skill file", description = "Delete a skill file by its file ID")
    public Result<Void> deleteSkillFile(
            @PathVariable @Min(1) Long fileId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        fileStorageService.deleteFile(fileId, userId);
        return Result.success();
    }
}
