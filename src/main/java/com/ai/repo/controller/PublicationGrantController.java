package com.ai.repo.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.repo.common.Result;
import com.ai.repo.dto.PublicationGrantCreateRequest;
import com.ai.repo.dto.PublicationGrantResponse;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.PublicationGrantService;

@RestController
@RequestMapping({"/api/publication-grants", "/api/agent-action-grants"})
public class PublicationGrantController {
    @Resource
    private PublicationGrantService publicationGrantService;

    @PostMapping
    @RequireAuth
    public ResponseEntity<Result<PublicationGrantResponse>> issue(
            @Valid @RequestBody PublicationGrantCreateRequest request,
            HttpServletRequest httpRequest) {
        if (httpRequest.getAttribute("agentId") != null) {
            throw new BusinessException(403, "Only an authenticated human user can issue action grants");
        }
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.ok(publicationGrantService.issue(userId, request));
    }
}
