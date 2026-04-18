package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.TempTokenGetResponse;
import com.ai.repo.dto.TempTokenStoreRequest;
import com.ai.repo.dto.TempTokenStoreResponse;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.TempTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth API", description = "Authentication and temporary token operations")
public class AuthController {

    @Resource
    private TempTokenService tempTokenService;

    @PostMapping("/temp-token")
    @RequireAuth
    @Operation(summary = "Store temporary token", description = "Store an access token temporarily with a session ID for Agent to retrieve")
    public Result<TempTokenStoreResponse> storeTempToken(@RequestBody TempTokenStoreRequest request) {
        String sessionId = tempTokenService.storeToken(request.getSessionId(), request.getAccessToken());

        TempTokenStoreResponse response = new TempTokenStoreResponse();
        response.setSessionId(sessionId);

        return Result.success(response);
    }

    @GetMapping("/temp-token/{sessionId}")
    @Operation(summary = "Get temporary token", description = "Retrieve and remove a temporary token by session ID (one-time use)")
    public Result<TempTokenGetResponse> getTempToken(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        String accessToken = tempTokenService.getAndRemoveToken(sessionId);

        if (accessToken == null) {
            return Result.error(404, "Token not found or expired");
        }

        TempTokenGetResponse response = new TempTokenGetResponse();
        response.setAccessToken(accessToken);

        return Result.success(response);
    }
}
