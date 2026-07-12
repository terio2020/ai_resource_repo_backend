package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.BugReportCreateRequest;
import com.ai.repo.dto.BugReportStatusUpdateRequest;
import com.ai.repo.dto.BugReportUpdateRequest;
import com.ai.repo.entity.BugReport;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.BugReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
@Validated
@Tag(name = "Bug Report API", description = "Bug report management for agents")
public class BugReportController {

    @Resource
    private BugReportService bugReportService;

    @PostMapping
    @ApiKeyAuth
    @Operation(summary = "Create a bug report", description = "Report a bug discovered by the agent")
    public ResponseEntity<Result<BugReport>> createBugReport(
            @Valid @RequestBody BugReportCreateRequest request,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        BugReport bugReport = new BugReport();
        bugReport.setAgentId(agentId);
        bugReport.setTitle(request.getTitle());
        bugReport.setDescription(request.getDescription());
        bugReport.setSeverity(request.getSeverity());
        bugReport.setSource(request.getSource());
        bugReport.setEnvironment(request.getEnvironment());
        bugReport.setStepsToReproduce(request.getStepsToReproduce());
        bugReport.setExpectedBehavior(request.getExpectedBehavior());
        bugReport.setActualBehavior(request.getActualBehavior());
        bugReport.setStackTrace(request.getStackTrace());
        bugReport.setCategory(request.getCategory());
        bugReport.setStatus("open");
        BugReport created = bugReportService.create(bugReport);
        return Result.ok(created);
    }

    @GetMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Get bug report by ID", description = "Retrieve a specific bug report by its ID")
    public ResponseEntity<Result<BugReport>> getBugReportById(
            @Parameter(description = "Bug report ID") @PathVariable @Min(1) Long id) {
        BugReport bugReport = bugReportService.findById(id);
        if (bugReport == null) {
            throw new BusinessException("Bug report not found");
        }
        return Result.ok(bugReport);
    }

    @GetMapping("/uid/{uid}")
    @ApiKeyAuth
    @Operation(summary = "Get bug report by UID", description = "Retrieve a specific bug report by its UID")
    public ResponseEntity<Result<BugReport>> getBugReportByUid(
            @Parameter(description = "Bug report UID") @PathVariable String uid) {
        BugReport bugReport = bugReportService.findByUid(uid);
        if (bugReport == null) {
            throw new BusinessException("Bug report not found");
        }
        return Result.ok(bugReport);
    }

    @GetMapping
    @ApiKeyAuth
    @Operation(summary = "List bug reports", description = "List bug reports with optional filters")
    public ResponseEntity<Result<List<BugReport>>> listBugReports(
            @Parameter(description = "Filter by agent ID") @RequestParam(required = false) Long agentId,
            @Parameter(description = "Filter by severity") @RequestParam(required = false) String severity,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category) {
        List<BugReport> bugReports = bugReportService.findWithFilters(agentId, severity, status, category);
        return Result.ok(bugReports);
    }

    @GetMapping("/agent/{agentId}")
    @ApiKeyAuth
    @Operation(summary = "Get bug reports by agent")
    public ResponseEntity<Result<List<BugReport>>> getBugReportsByAgent(
            @Parameter(description = "Agent ID") @PathVariable @Min(1) Long agentId) {
        List<BugReport> bugReports = bugReportService.findByAgentId(agentId);
        return Result.ok(bugReports);
    }

    @PutMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Update a bug report")
    public ResponseEntity<Result<BugReport>> updateBugReport(
            @Parameter(description = "Bug report ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody BugReportUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        BugReport existing = bugReportService.findById(id);
        if (existing == null) {
            throw new BusinessException("Bug report not found");
        }
        if (!existing.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "Only the reporting agent can update this bug report");
        }
        BugReport bugReport = new BugReport();
        bugReport.setId(id);
        bugReport.setTitle(request.getTitle());
        bugReport.setDescription(request.getDescription());
        bugReport.setSeverity(request.getSeverity());
        bugReport.setSource(request.getSource());
        bugReport.setEnvironment(request.getEnvironment());
        bugReport.setStepsToReproduce(request.getStepsToReproduce());
        bugReport.setExpectedBehavior(request.getExpectedBehavior());
        bugReport.setActualBehavior(request.getActualBehavior());
        bugReport.setStackTrace(request.getStackTrace());
        bugReport.setCategory(request.getCategory());
        bugReport.setStatus(request.getStatus());
        BugReport updated = bugReportService.update(bugReport);
        return Result.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @ApiKeyAuth
    @Operation(summary = "Update bug report status")
    public ResponseEntity<Result<Void>> updateBugReportStatus(
            @Parameter(description = "Bug report ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody BugReportStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        BugReport existing = bugReportService.findById(id);
        if (existing == null) {
            throw new BusinessException("Bug report not found");
        }
        if (!existing.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "Only the reporting agent can update this bug report");
        }
        bugReportService.updateStatus(id, request.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete a bug report")
    public ResponseEntity<Result<Void>> deleteBugReport(
            @Parameter(description = "Bug report ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        BugReport existing = bugReportService.findById(id);
        if (existing == null) {
            throw new BusinessException("Bug report not found");
        }
        if (!existing.getAgentId().equals(agentId)) {
            throw new BusinessException(403, "Only the reporting agent can delete this bug report");
        }
        bugReportService.delete(id);
        return Result.ok();
    }
}