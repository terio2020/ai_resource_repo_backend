package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.entity.Notification;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Validated
@Tag(name = "Notification API", description = "Notification management operations")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @GetMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by its ID")
    public ResponseEntity<Result<Notification>> getNotificationById(
            @Parameter(description = "Notification ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Notification notification = notificationService.findById(id);

        if (notification == null) {
            return Result.fail(404, "Notification not found");
        }

        if (!notification.getAgentId().equals(agentId)) {
            return Result.fail(403, "Access denied");
        }

        return Result.ok(notification);
    }

    @GetMapping("/uid/{uid}")
    @ApiKeyAuth
    @Operation(summary = "Get notification by UID")
    public ResponseEntity<Result<Notification>> getNotificationByUid(
            @Parameter(description = "Notification UID") @PathVariable String uid,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Notification notification = notificationService.findByUid(uid);

        if (notification == null) {
            return Result.fail(404, "Notification not found");
        }

        if (!notification.getAgentId().equals(agentId)) {
            return Result.fail(403, "Access denied");
        }

        return Result.ok(notification);
    }

    @GetMapping
    @ApiKeyAuth
    @Operation(summary = "Get notifications", description = "Retrieve notifications for authenticated agent")
    public ResponseEntity<Result<List<Notification>>> getNotifications(
            @Parameter(description = "Filter unread only") @RequestParam(required = false) Boolean unread,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");

        List<Notification> notifications;
        if (Boolean.TRUE.equals(unread)) {
            notifications = notificationService.findUnreadByAgentId(agentId);
        } else {
            notifications = notificationService.findByAgentId(agentId);
        }

        return Result.ok(notifications);
    }

    @GetMapping("/count/unread")
    @ApiKeyAuth
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<Result<Long>> getUnreadCount(HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Long count = notificationService.countUnreadByAgentId(agentId);
        return Result.ok(count);
    }

    @PostMapping("/{id}/read")
    @ApiKeyAuth
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    public ResponseEntity<Result<Void>> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Notification notification = notificationService.findById(id);

        if (notification == null) {
            return Result.fail(404, "Notification not found");
        }

        if (!notification.getAgentId().equals(agentId)) {
            return Result.fail(403, "Access denied");
        }

        notificationService.markAsRead(id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    @ApiKeyAuth
    @Operation(summary = "Mark all as read", description = "Mark all notifications for authenticated agent as read")
    public ResponseEntity<Result<Void>> markAllAsRead(HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        notificationService.markAllAsRead(agentId);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete notification", description = "Delete a specific notification")
    public ResponseEntity<Result<Void>> deleteNotification(
            @Parameter(description = "Notification ID") @PathVariable @Min(1) Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Notification notification = notificationService.findById(id);

        if (notification == null) {
            return Result.fail(404, "Notification not found");
        }

        if (!notification.getAgentId().equals(agentId)) {
            return Result.fail(403, "Access denied");
        }

        notificationService.delete(id);
        return Result.ok();
    }
}
