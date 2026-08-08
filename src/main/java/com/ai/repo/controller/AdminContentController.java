package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.common.Result;
import com.ai.repo.dto.AdminBugReportStatusUpdateRequest;
import com.ai.repo.dto.AdminContentStatusUpdateRequest;
import com.ai.repo.entity.BugReport;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.entity.AgentPackage;
import com.ai.repo.security.RequireAdmin;
import com.ai.repo.service.AdminContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content")
@Validated
@Tag(name = "Admin Content API", description = "Admin-only content governance and bug report management")
public class AdminContentController {

    @Resource
    private AdminContentService adminContentService;

    @GetMapping("/memories")
    @RequireAdmin
    @Operation(summary = "List memories", description = "Admin-only paginated memory list for moderation")
    public ResponseEntity<Result<PageResult<Memory>>> listMemories(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(adminContentService.listMemories(page, size, keyword, status));
    }

    @GetMapping("/skill-repos")
    @RequireAdmin
    @Operation(summary = "List skill repositories", description = "Admin-only paginated skill repository list for moderation")
    public ResponseEntity<Result<PageResult<SkillRepository>>> listSkillRepos(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(adminContentService.listSkillRepos(page, size, keyword, status));
    }

    @GetMapping("/packages")
    @RequireAdmin
    @Operation(summary = "List packages", description = "Admin-only paginated package list for moderation")
    public ResponseEntity<Result<PageResult<AgentPackage>>> listPackages(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(adminContentService.listPackages(page, size, keyword, status));
    }

    @PatchMapping("/memories/{id}/status")
    @RequireAdmin
    @Operation(summary = "Update memory status", description = "Admin-only status update (VISIBLE or BANNED)")
    public ResponseEntity<Result<Void>> updateMemoryStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminContentStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        adminContentService.updateMemoryStatus(operatorId, id, request.getStatus());
        return Result.ok();
    }

    @PatchMapping("/skill-repos/{id}/status")
    @RequireAdmin
    @Operation(summary = "Update skill repository status", description = "Admin-only status update (VISIBLE or BANNED)")
    public ResponseEntity<Result<Void>> updateSkillRepoStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminContentStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        adminContentService.updateSkillRepoStatus(operatorId, id, request.getStatus());
        return Result.ok();
    }

    @PatchMapping("/packages/{id}/status")
    @RequireAdmin
    @Operation(summary = "Update package status", description = "Admin-only status update (VISIBLE or BANNED)")
    public ResponseEntity<Result<Void>> updatePackageStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminContentStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        adminContentService.updatePackageStatus(operatorId, id, request.getStatus());
        return Result.ok();
    }

    @GetMapping("/bug-reports")
    @RequireAdmin
    @Operation(summary = "List bug reports", description = "Admin-only paginated bug report list with optional status/severity filters")
    public ResponseEntity<Result<PageResult<BugReport>>> listBugReports(
            @Parameter(description = "Page number, default 1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Page size, default 10, max 100")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Status filter: open, in_progress, resolved, closed")
            @RequestParam(required = false) String status,
            @Parameter(description = "Severity filter: info, low, medium, high, critical")
            @RequestParam(required = false) String severity) {
        return Result.ok(adminContentService.listBugReports(page, size, status, severity));
    }

    @GetMapping("/bug-reports/{id}")
    @RequireAdmin
    @Operation(summary = "Get bug report", description = "Admin-only bug report detail")
    public ResponseEntity<Result<BugReport>> getBugReport(@PathVariable @Min(1) Long id) {
        return Result.ok(adminContentService.getBugReport(id));
    }

    @PatchMapping("/bug-reports/{id}/status")
    @RequireAdmin
    @Operation(summary = "Update bug report status", description = "Admin-only status update (open, in_progress, resolved, closed)")
    public ResponseEntity<Result<Void>> updateBugReportStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminBugReportStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        adminContentService.updateBugReportStatus(operatorId, id, request.getStatus());
        return Result.ok();
    }
}
