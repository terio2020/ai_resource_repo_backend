package com.ai.repo.service;

import java.time.LocalDateTime;
import java.util.List;

import com.ai.repo.dto.FileTreeEntry;

public interface SkillPublicationRequestService {
    record Created(String requestId, String approvalUrl, Long repositoryId,
                   LocalDateTime expiresAt) {}

    record Details(String status, Long repositoryId, Long agentId, String skillName, String version,
                   String description, String headCommit, List<FileTreeEntry> files,
                   LocalDateTime expiresAt) {}

    record Status(String status, Long repositoryId, LocalDateTime expiresAt) {}

    Created create(Long agentId, Long userId, Long repositoryId);

    Details details(String requestId, Long userId);

    Status status(String requestId, Long agentId);

    void approve(String requestId, Long userId);

    void reject(String requestId, Long userId);
}
