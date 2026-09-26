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

import com.ai.repo.aspect.RateLimit;
import com.ai.repo.common.Result;
import com.ai.repo.dto.SkillUploadRequestCreateRequest;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.AgentMutationPolicy;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.SkillPublicationRequestService;
import com.ai.repo.service.SkillUploadRequestService;

@RestController
@RequestMapping("/api/skill-upload-requests")
public class SkillUploadRequestController {
    @Resource private SkillUploadRequestService uploadService;
    @Resource private SkillPublicationRequestService publicationService;

    @PostMapping
    @ApiKeyAuth
    @RateLimit(value = 10, period = 3600)
    public ResponseEntity<Result<SkillUploadRequestService.Created>> create(
            @Valid @RequestBody SkillUploadRequestCreateRequest body, HttpServletRequest request) {
        AgentMutationPolicy.requireCurrent(request);
        throw new BusinessException(410, "Private uploads no longer need approval; create a private Skill and push with the Agent key");
    }

    @GetMapping("/{requestId}")
    @RequireAuth
    public ResponseEntity<Result<SkillUploadRequestService.Details>> details(
            @PathVariable String requestId, HttpServletRequest request) {
        return Result.ok(uploadService.details(requestId, humanUserId(request)));
    }

    @PostMapping("/{requestId}/approve")
    @RequireAuth
    public ResponseEntity<Result<Void>> approve(
            @PathVariable String requestId, HttpServletRequest request) {
        uploadService.approve(requestId, humanUserId(request));
        return Result.okMessage("Private Skill upload approved");
    }

    @PostMapping("/{requestId}/reject")
    @RequireAuth
    public ResponseEntity<Result<Void>> reject(
            @PathVariable String requestId, HttpServletRequest request) {
        uploadService.reject(requestId, humanUserId(request));
        return Result.okMessage("Private Skill upload rejected");
    }

    @GetMapping("/{requestId}/status")
    @ApiKeyAuth
    public ResponseEntity<Result<SkillUploadRequestService.Status>> status(
            @PathVariable String requestId, HttpServletRequest request) {
        return Result.ok(uploadService.status(requestId, (Long) request.getAttribute("agentId")));
    }

    @PostMapping("/{requestId}/publication-link")
    @RequireAuth
    @RateLimit(value = 5, period = 3600)
    public ResponseEntity<Result<SkillPublicationRequestService.Created>> publicationLink(
            @PathVariable String requestId, HttpServletRequest request) {
        Long userId = humanUserId(request);
        Long repositoryId = uploadService.publicationReadyRepository(requestId, userId);
        Long agentId = uploadService.details(requestId, userId).agentId();
        return Result.ok(publicationService.create(agentId, userId, repositoryId));
    }

    private Long humanUserId(HttpServletRequest request) {
        if (request.getAttribute("agentId") != null) {
            throw new BusinessException(403, "Only the owning human can approve Skill uploads");
        }
        return (Long) request.getAttribute("userId");
    }
}
