package com.ai.repo.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ai.repo.aspect.RateLimit;
import com.ai.repo.common.Result;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.security.AgentMutationPolicy;
import com.ai.repo.security.ApiKeyAuth;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.MemoryPublicationRequestService;

@RestController
@Validated
@RequestMapping("/api/memory-publication-requests")
public class MemoryPublicationRequestController {
    public record CreateRequest(@NotNull @Min(1) Long memoryId) {}
    @Resource private MemoryPublicationRequestService service;

    @PostMapping
    @ApiKeyAuth
    @RateLimit(value = 10, period = 3600)
    public ResponseEntity<Result<MemoryPublicationRequestService.Created>> create(
            @Valid @RequestBody CreateRequest body, HttpServletRequest request) {
        AgentMutationPolicy.requireCurrent(request);
        return Result.ok(service.create((Long) request.getAttribute("agentId"),
                (Long) request.getAttribute("userId"), body.memoryId()));
    }
    @GetMapping("/{requestId}")
    @RequireAuth
    public ResponseEntity<Result<MemoryPublicationRequestService.Details>> details(
            @PathVariable String requestId, HttpServletRequest request) {
        return Result.ok(service.details(requestId, human(request)));
    }
    @GetMapping("/{requestId}/status")
    @ApiKeyAuth
    public ResponseEntity<Result<MemoryPublicationRequestService.Status>> status(
            @PathVariable String requestId, HttpServletRequest request) {
        return Result.ok(service.status(requestId, (Long) request.getAttribute("agentId")));
    }
    @PostMapping("/{requestId}/approve")
    @RequireAuth
    public ResponseEntity<Result<Void>> approve(@PathVariable String requestId, HttpServletRequest request) {
        service.approve(requestId, human(request));
        return Result.okMessage("Memory published");
    }
    @PostMapping("/{requestId}/reject")
    @RequireAuth
    public ResponseEntity<Result<Void>> reject(@PathVariable String requestId, HttpServletRequest request) {
        service.reject(requestId, human(request));
        return Result.okMessage("Memory publication rejected");
    }
    private Long human(HttpServletRequest request) {
        if (request.getAttribute("agentId") != null) throw new BusinessException(403, "Only the owning human can confirm publication");
        return (Long) request.getAttribute("userId");
    }
}
