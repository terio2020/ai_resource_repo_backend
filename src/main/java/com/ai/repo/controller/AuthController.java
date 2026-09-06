package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.dto.TempTokenGetResponse;
import com.ai.repo.dto.TempTokenStoreRequest;
import com.ai.repo.dto.TempTokenStoreResponse;
import com.ai.repo.dto.TempTokenRetrieveRequest;
import com.ai.repo.security.RequireAuth;
import com.ai.repo.service.TempTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<Result<TempTokenStoreResponse>> storeTempToken(
            @RequestBody TempTokenStoreRequest request,
            HttpServletRequest httpRequest) {
        if (httpRequest.getAttribute("agentId") != null) {
            return Result.fail(403, "Human user authentication required to store a temporary token");
        }
        String sessionId = tempTokenService.storeToken(request.getSessionId(), request.getAccessToken());

        TempTokenStoreResponse response = new TempTokenStoreResponse();
        response.setSessionId(sessionId);

        return Result.ok(response);
    }

    @GetMapping("/temp-token")
    @Operation(summary = "Get temporary token (legacy)", description = "Compatibility-only query form. Prefer POST /temp-token/retrieve so sessionId is not placed in the URL.")
    public ResponseEntity<Result<TempTokenGetResponse>> getTempToken(
            @Parameter(description = "Session ID") @RequestParam(value = "sessionId", required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Result.fail(400, "Session ID is required");
        }
        return retrieveToken(sessionId);
    }

    @PostMapping("/temp-token/retrieve")
    @Operation(summary = "Retrieve temporary token", description = "Retrieve and remove a temporary token once using a secret session ID in the request body")
    public ResponseEntity<Result<TempTokenGetResponse>> retrieveTempToken(
            @Valid @RequestBody TempTokenRetrieveRequest request) {
        return retrieveToken(request.getSessionId());
    }

    private ResponseEntity<Result<TempTokenGetResponse>> retrieveToken(String sessionId) {
        String accessToken = tempTokenService.getAndRemoveToken(sessionId);

        if (accessToken == null) {
            return Result.fail(404, "Token not found or expired");
        }

        TempTokenGetResponse response = new TempTokenGetResponse();
        response.setAccessToken(accessToken);

        return Result.ok(response);
    }
}
