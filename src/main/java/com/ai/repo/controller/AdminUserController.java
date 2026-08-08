package com.ai.repo.controller;

import com.ai.repo.common.PageResult;
import com.ai.repo.common.Result;
import com.ai.repo.dto.AdminUserResponse;
import com.ai.repo.dto.AdminUserRoleUpdateRequest;
import com.ai.repo.dto.AdminUserStatusUpdateRequest;
import com.ai.repo.security.RequireAdmin;
import com.ai.repo.service.AdminUserService;
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
@RequestMapping("/api/admin/users")
@Validated
@Tag(name = "Admin User API", description = "Admin-only user management")
public class AdminUserController {

    @Resource
    private AdminUserService adminUserService;

    @GetMapping
    @RequireAdmin
    @Operation(summary = "List users", description = "Admin-only paginated user list with optional keyword/role/status filters")
    public ResponseEntity<Result<PageResult<AdminUserResponse>>> listUsers(
            @Parameter(description = "Page number, default 1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Page size, default 10, max 100")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Search keyword matching username/email/nickname")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Role filter: USER or ADMIN")
            @RequestParam(required = false) String role,
            @Parameter(description = "Status filter: ACTIVE or DISABLED")
            @RequestParam(required = false) String status) {
        return Result.ok(adminUserService.listUsers(page, size, keyword, role, status));
    }

    @PatchMapping("/{id}/role")
    @RequireAdmin
    @Operation(summary = "Update user role", description = "Admin-only role update (USER or ADMIN); cannot change own role")
    public ResponseEntity<Result<Void>> updateRole(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminUserRoleUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        adminUserService.updateRole(operatorId, id, request.getRole());
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    @RequireAdmin
    @Operation(summary = "Update user status", description = "Admin-only status update (ACTIVE or DISABLED); disabling cascades to the user's agents; cannot disable self or the last active admin")
    public ResponseEntity<Result<Void>> updateStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminUserStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        adminUserService.updateStatus(operatorId, id, request.getStatus());
        return Result.ok();
    }
}
