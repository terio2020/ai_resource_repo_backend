package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.Notification;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification API", description = "Notification management operations")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @GetMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by its ID")
    public Result<Notification> getNotificationById(
            @Parameter(description = "Notification ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Notification notification = notificationService.findById(id);

        if (notification == null) {
            return Result.error(404, "Notification not found");
        }

        if (!notification.getAgentId().equals(agentId)) {
            return Result.error(403, "Access denied");
        }

        return Result.success(notification);
    }

    @GetMapping
    @ApiKeyAuth
    @Operation(summary = "Get notifications", description = "Retrieve notifications for authenticated agent")
    public Result<List<Notification>> getNotifications(
            @Parameter(description = "Filter unread only") @RequestParam(required = false) Boolean unread,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");

        List<Notification> notifications;
        if (Boolean.TRUE.equals(unread)) {
            notifications = notificationService.findUnreadByAgentId(agentId);
        } else {
            notifications = notificationService.findByAgentId(agentId);
        }

        return Result.success(notifications);
    }

    @GetMapping("/count/unread")
    @ApiKeyAuth
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public Result<Long> getUnreadCount(HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Long count = notificationService.countUnreadByAgentId(agentId);
        return Result.success(count);
    }

    @PostMapping("/{id}/read")
    @ApiKeyAuth
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    public Result<Void> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Notification notification = notificationService.findById(id);

        if (notification == null) {
            return Result.error(404, "Notification not found");
        }

        if (!notification.getAgentId().equals(agentId)) {
            return Result.error(403, "Access denied");
        }

        notificationService.markAsRead(id);
        return Result.success();
    }

    @PostMapping("/read-all")
    @ApiKeyAuth
    @Operation(summary = "Mark all as read", description = "Mark all notifications for authenticated agent as read")
    public Result<Void> markAllAsRead(HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        notificationService.markAllAsRead(agentId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiKeyAuth
    @Operation(summary = "Delete notification", description = "Delete a specific notification")
    public Result<Void> deleteNotification(
            @Parameter(description = "Notification ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Notification notification = notificationService.findById(id);

        if (notification == null) {
            return Result.error(404, "Notification not found");
        }

        if (!notification.getAgentId().equals(agentId)) {
            return Result.error(403, "Access denied");
        }

        notificationService.delete(id);
        return Result.success();
    }
}
