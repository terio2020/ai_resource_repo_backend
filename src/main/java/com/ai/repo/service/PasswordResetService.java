package com.ai.repo.service;

import com.ai.repo.dto.PasswordResetConfirmRequest;

public interface PasswordResetService {

    /**
     * Request password reset - send email with reset link
     * @param email user email address
     */
    void requestPasswordReset(String email);

    /**
     * Validate reset token - check if token is valid and not expired
     * @param token reset token from email link
     * @return true if token is valid
     */
    boolean validateResetToken(String token);

    /**
     * Confirm password reset - set new password
     * @param request contains token and new password
     */
    void confirmPasswordReset(PasswordResetConfirmRequest request);

    /**
     * Invalidate all existing sessions for a user (after password change)
     * @param userId user id
     */
    void invalidateUserSessions(Long userId);
}