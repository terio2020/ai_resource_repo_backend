package com.ai.repo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.repo.common.Result;
import com.ai.repo.dto.AdminEmailRequest;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.RequireAdmin;
import com.ai.repo.service.AdminEmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/notifications")
@Validated
@Tag(name = "Admin Notification API", description = "Admin-only outbound notifications")
public class AdminNotificationController {

    @Resource
    private AdminEmailService adminEmailService;

    @PostMapping("/email")
    @RequireAdmin
    @Operation(summary = "Send an administrator email",
            description = "Queues a custom email to every ACTIVE ADMIN account")
    public ResponseEntity<Result<Void>> sendEmail(@Valid @RequestBody AdminEmailRequest request) {
        validateActionUrl(request.getActionUrl());
        adminEmailService.sendToActiveAdmins(request.getSubject(), request.getBody(), request.getActionUrl());
        return Result.ok();
    }

    private void validateActionUrl(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank() || actionUrl.startsWith("/")) {
            return;
        }
        if (!actionUrl.startsWith("https://logicomanet.com/")) {
            throw new BusinessException(400, "Action URL must be a relative path or logicomanet.com URL");
        }
    }
}
