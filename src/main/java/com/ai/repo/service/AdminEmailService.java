package com.ai.repo.service;

public interface AdminEmailService {
    void sendToActiveAdmins(String subject, String body, String actionUrl);

    void sendToAdminOwner(Long userId, String subject, String body, String actionUrl);
}
