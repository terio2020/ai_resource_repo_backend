package com.ai.repo.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.repo.common.Result;
import com.ai.repo.aspect.RateLimit;
import com.ai.repo.dto.SkillPublicationRequestCreateRequest;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.AgentMutationPolicy;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.SkillPublicationRequestService;

@RestController
@RequestMapping("/api/skill-publication-requests")
public class SkillPublicationRequestController {
    @Resource
    private SkillPublicationRequestService requestService;

    @PostMapping
    @ApiKeyAuth
    @RateLimit(value = 10, period = 3600)
    public ResponseEntity<Result<SkillPublicationRequestService.Created>> create(
            @Valid @RequestBody SkillPublicationRequestCreateRequest body,
            HttpServletRequest request) {
        AgentMutationPolicy.requireCurrent(request);
        return Result.ok(requestService.create(
                (Long) request.getAttribute("agentId"),
                (Long) request.getAttribute("userId"), body.getRepositoryId()));
    }

    @GetMapping("/{requestId}")
    @RequireAuth
    public ResponseEntity<Result<SkillPublicationRequestService.Details>> details(
            @PathVariable String requestId, HttpServletRequest request) {
        return Result.ok(requestService.details(requestId, humanUserId(request)));
    }

    @PostMapping("/{requestId}/approve")
    @RequireAuth
    public ResponseEntity<Result<Void>> approve(
            @PathVariable String requestId, HttpServletRequest request) {
        requestService.approve(requestId, humanUserId(request));
        return Result.okMessage("Skill published");
    }

    @PostMapping("/{requestId}/reject")
    @RequireAuth
    public ResponseEntity<Result<Void>> reject(
            @PathVariable String requestId, HttpServletRequest request) {
        requestService.reject(requestId, humanUserId(request));
        return Result.okMessage("Publication rejected");
    }

    @GetMapping("/{requestId}/status")
    @ApiKeyAuth
    public ResponseEntity<Result<SkillPublicationRequestService.Status>> status(
            @PathVariable String requestId, HttpServletRequest request) {
        return Result.ok(requestService.status(requestId,
                (Long) request.getAttribute("agentId")));
    }

    private Long humanUserId(HttpServletRequest request) {
        if (request.getAttribute("agentId") != null) {
            throw new BusinessException(403, "Only an authenticated human user can decide publication");
        }
        return (Long) request.getAttribute("userId");
    }
}
