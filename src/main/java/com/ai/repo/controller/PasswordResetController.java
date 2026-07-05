package com.ai.repo.controller;
import org.springframework.http.ResponseEntity;

import com.ai.repo.common.Result;
import com.ai.repo.dto.PasswordResetConfirmRequest;
import com.ai.repo.dto.PasswordResetRequest;
import com.ai.repo.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/password")
@Tag(name = "Password Reset API", description = "Password recovery and reset operations")
public class PasswordResetController {

    @Resource
    private PasswordResetService passwordResetService;

    @PostMapping("/reset-request")
    @Operation(summary = "Request password reset", description = "Send password reset email to user's email address")
    public ResponseEntity<Result<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        // Always return success to prevent account enumeration
        return Result.okMessage("If an account exists with this email, a password reset link has been sent");
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate reset token", description = "Check if a password reset token is valid")
    public ResponseEntity<Result<Boolean>> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordResetService.validateResetToken(token);
        return Result.ok(isValid);
    }

    @PostMapping("/reset-confirm")
    @Operation(summary = "Confirm password reset", description = "Reset password using valid token")
    public ResponseEntity<Result<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmPasswordReset(request);
        return Result.okMessage("Password has been reset successfully");
    }
}