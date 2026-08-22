package com.ai.repo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.repo.aspect.RateLimit;
import com.ai.repo.common.Result;
import com.ai.repo.dto.AdminEmailRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.service.AdminEmailService;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agent/notifications")
@Validated
@Tag(name = "Agent Administrator Notification API",
        description = "Restricted email delivery from an Agent to its administrator owner")
public class AgentAdminNotificationController {

    @Resource
    private AgentService agentService;
    @Resource
    private UserService userService;
    @Resource
    private AdminEmailService adminEmailService;

    @PostMapping("/email")
    @ApiKeyAuth
    @RateLimit(value = 6, period = 3600)
    @Operation(summary = "Email the Agent's administrator owner",
            description = "Only an Agent owned by an ACTIVE ADMIN account may use this endpoint")
    public ResponseEntity<Result<Void>> sendEmail(
            @Valid @RequestBody AdminEmailRequest request,
            HttpServletRequest httpRequest) {
        Long agentId = (Long) httpRequest.getAttribute("agentId");
        Agent agent = agentId == null ? null : agentService.findById(agentId);
        if (agent == null || agent.getUserId() == null) {
            throw new BusinessException(403, "Administrator-owned Agent required");
        }
        User owner = userService.findById(agent.getUserId());
        if (owner == null || !"ADMIN".equals(owner.getRole()) || !"ACTIVE".equals(owner.getStatus())) {
            throw new BusinessException(403, "Administrator-owned Agent required");
        }
        validateActionUrl(request.getActionUrl());
        String agentName = agent.getDisplayName() == null || agent.getDisplayName().isBlank()
                ? agent.getName() : agent.getDisplayName();
        String subject = "[Agent: " + agentName + "] " + request.getSubject();
        String body = "Agent " + agentName + " (ID " + agentId + ") sent this notification.\n\n"
                + request.getBody();
        adminEmailService.sendToAdminOwner(owner.getId(), subject, body, request.getActionUrl());
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
