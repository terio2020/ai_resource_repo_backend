package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.common.Result;
import com.ai.repo.dto.AdminAgentResponse;
import com.ai.repo.dto.AdminAgentStatusUpdateRequest;
import com.ai.repo.security.RequireAdmin;
import com.ai.repo.service.AdminAgentService;
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
@RequestMapping("/api/admin/agents")
@Validated
@Tag(name = "Admin Agent API", description = "Admin-only agent management")
public class AdminAgentController {

    @Resource
    private AdminAgentService adminAgentService;

    @GetMapping
    @RequireAdmin
    @Operation(summary = "List agents", description = "Admin-only paginated agent list with optional keyword/status/type filters")
    public ResponseEntity<Result<PageResult<AdminAgentResponse>>> listAgents(
            @Parameter(description = "Page number, default 1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Page size, default 10, max 100")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Search keyword matching agent name")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Status filter: ACTIVE/IDLE/BUSY/OFFLINE/DISABLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Type filter")
            @RequestParam(required = false) String type) {
        return Result.ok(adminAgentService.listAgents(page, size, keyword, status, type));
    }

    @PatchMapping("/{id}/status")
    @RequireAdmin
    @Operation(summary = "Update agent status", description = "Admin-only status update (ACTIVE/IDLE/BUSY/OFFLINE/DISABLED); DISABLED removes all agent capabilities")
    public ResponseEntity<Result<Void>> updateStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminAgentStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        adminAgentService.updateStatus(operatorId, id, request.getStatus());
        return Result.ok();
    }
}
