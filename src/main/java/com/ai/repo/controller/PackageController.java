package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.*;
import com.ai.repo.entity.AgentPackage;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.PackageService;
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
@RequestMapping("/api/packages")
@Validated
@Tag(name = "Package API", description = "Agent package management (skill / memory)")
public class PackageController {

    @Resource
    private PackageService packageService;

    @PostMapping
    @RequireAuth
    @Operation(summary = "Create a package", description = "Create a new skill or memory package for an agent.")
    public Result<PackageResponse> create(
            @Valid @RequestBody PackageCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AgentPackage ap = packageService.create(userId, request.getAgentId(), request);
        return Result.success("Package created", toResponse(ap));
    }

    @PutMapping("/{id}")
    @RequireAuth
    @Operation(summary = "Update package metadata", description = "Update description and tags of a package.")
    public Result<PackageResponse> update(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody PackageUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AgentPackage ap = packageService.update(id, userId, request);
        return Result.success(toResponse(ap));
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @Operation(summary = "Delete a package", description = "Delete the package and all its version files.")
    public Result<Void> delete(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        packageService.delete(id, userId);
        return Result.success("Package deleted");
    }

    @GetMapping("/{id}")
    @RequireAuth
    @Operation(summary = "Get package by ID", description = "Retrieve package metadata.")
    public Result<PackageResponse> getById(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id) {
        AgentPackage ap = packageService.findById(id);
        return Result.success(toResponse(ap));
    }

    @GetMapping("/public")
    @RequireAuth
    @Operation(summary = "List public packages", description = "Browse all public packages with pagination.")
    public Result<List<PackageResponse>> listPublic(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        List<PackageResponse> list = packageService.listPublic(page, size);
        return Result.success(list);
    }

    @GetMapping("/search")
    @RequireAuth
    @Operation(summary = "Search packages", description = "Search packages by keyword (name, description, tags).")
    public Result<List<PackageResponse>> search(
            @Parameter(description = "Search keyword") @RequestParam("q") String keyword) {
        List<PackageResponse> list = packageService.search(keyword);
        return Result.success(list);
    }

    @GetMapping("/agent/{agentId}")
    @RequireAuth
    @Operation(summary = "List packages by agent", description = "Get all packages owned by an agent.")
    public Result<List<PackageResponse>> listByAgent(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {
        List<PackageResponse> list = packageService.listByAgent(agentId);
        return Result.success(list);
    }

    @GetMapping("/user/{userId}")
    @RequireAuth
    @Operation(summary = "List packages by user", description = "Get all packages created by a user.")
    public Result<List<PackageResponse>> listByUser(
            @Parameter(description = "User ID") @PathVariable @Min(1) Long userId) {
        List<PackageResponse> list = packageService.listByUser(userId);
        return Result.success(list);
    }

    @PostMapping("/{id}/versions")
    @RequireAuth
    @Operation(summary = "Publish a new version", description = "Upload files as a new version of the package.")
    public Result<PackageVersionResponse> publishVersion(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "Commit message") @RequestParam String commitMessage,
            @Parameter(description = "Files to upload") @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PackageVersionResponse version = packageService.publishVersion(id, userId, commitMessage, files);
        return Result.success("Version published", version);
    }

    @GetMapping("/{id}/versions")
    @RequireAuth
    @Operation(summary = "List versions", description = "List all versions of a package.")
    public Result<List<PackageVersionResponse>> getVersions(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id) {
        List<PackageVersionResponse> versions = packageService.getVersions(id);
        return Result.success(versions);
    }

    @GetMapping("/versions/{versionId}")
    @RequireAuth
    @Operation(summary = "Get version detail", description = "Get version metadata and file list.")
    public Result<PackageVersionResponse> getVersionDetail(
            @Parameter(description = "Version ID") @PathVariable @Min(1) Long versionId) {
        PackageVersionResponse detail = packageService.getVersionDetail(versionId);
        return Result.success(detail);
    }

    @GetMapping("/versions/{versionId}/files")
    @RequireAuth
    @Operation(summary = "List version files", description = "List all files in a version.")
    public Result<List<PackageFileResponse>> getVersionFiles(
            @Parameter(description = "Version ID") @PathVariable @Min(1) Long versionId) {
        List<PackageFileResponse> files = packageService.getVersionFiles(versionId);
        return Result.success(files);
    }

    @GetMapping("/files/{fileId}/download")
    @RequireAuth
    @Operation(summary = "Download a file", description = "Download a single file from a version.")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @Parameter(description = "File ID") @PathVariable @Min(1) Long fileId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        org.springframework.core.io.Resource resource = packageService.downloadFile(fileId, userId, userId, agentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/versions/{versionId}/download")
    @RequireAuth
    @Operation(summary = "Download version as ZIP", description = "Download all files of a version as a ZIP archive.")
    public ResponseEntity<org.springframework.core.io.Resource> downloadArchive(
            @Parameter(description = "Version ID") @PathVariable @Min(1) Long versionId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        org.springframework.core.io.Resource resource = packageService.downloadArchive(versionId, userId, userId, agentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"package_v" + versionId + ".zip\"")
                .body(resource);
    }

    @PatchMapping("/{id}/visibility")
    @RequireAuth
    @Operation(summary = "Toggle visibility", description = "Set a package as public or private.")
    public Result<Void> setVisibility(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            @Parameter(description = "New visibility") @RequestParam boolean isPublic,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        packageService.setVisibility(id, userId, isPublic);
        return Result.success(isPublic ? "Package set to public" : "Package set to private");
    }

    @PostMapping("/{id}/rollback")
    @RequireAuth
    @Operation(summary = "Rollback to a version", description = "Rollback the package to a previous version.")
    public Result<Void> rollback(
            @Parameter(description = "Package ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody RollbackRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        packageService.rollback(id, userId, request.getVersionId());
        return Result.success("Rolled back successfully");
    }

    private PackageResponse toResponse(AgentPackage ap) {
        PackageResponse r = new PackageResponse();
        r.setId(ap.getId());
        r.setUserId(ap.getUserId());
        r.setAgentId(ap.getAgentId());
        r.setPackageType(ap.getPackageType());
        r.setName(ap.getName());
        r.setDescription(ap.getDescription());
        r.setTags(ap.getTags());
        r.setIsPublic(ap.getIsPublic());
        r.setCurrentVersionId(ap.getCurrentVersionId());
        r.setDownloadCount(ap.getDownloadCount());
        r.setCreatedAt(ap.getCreatedAt());
        r.setUpdatedAt(ap.getUpdatedAt());
        return r;
    }
}
