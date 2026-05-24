package com.ai.repo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Password reset confirmation - set new password")
public class PasswordResetConfirmRequest {

    @Schema(description = "Password reset token", example = "abc123...")
    @NotBlank(message = "Token is required")
    private String token;

    @Schema(description = "New password", example = "NewPassword123!")
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String newPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}