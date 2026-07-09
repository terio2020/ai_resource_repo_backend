package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

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
    public ResponseEntity<Result<TempTokenStoreResponse>> storeTempToken(@RequestBody TempTokenStoreRequest request) {
        String sessionId = tempTokenService.storeToken(request.getSessionId(), request.getAccessToken());

        TempTokenStoreResponse response = new TempTokenStoreResponse();
        response.setSessionId(sessionId);

        return Result.ok(response);
    }

    @GetMapping({"/temp-token/{sessionId}", "/temp-token"})
    @Operation(summary = "Get temporary token", description = "Retrieve and remove a temporary token by session ID (one-time use, sessionId is the secret)")
    public ResponseEntity<Result<TempTokenGetResponse>> getTempToken(
            @Parameter(description = "Session ID (path variable)") @PathVariable(required = false) String sessionId,
            @Parameter(description = "Session ID (query param)") @RequestParam(value = "sessionId", required = false) String sessionIdParam) {
        String resolved = sessionId != null ? sessionId : sessionIdParam;
        if (resolved == null) {
            return Result.fail(400, "Session ID is required");
        }
        String accessToken = tempTokenService.getAndRemoveToken(resolved);

        if (accessToken == null) {
            return Result.fail(404, "Token not found or expired");
        }

        TempTokenGetResponse response = new TempTokenGetResponse();
        response.setAccessToken(accessToken);

        return Result.ok(response);
    }
}
