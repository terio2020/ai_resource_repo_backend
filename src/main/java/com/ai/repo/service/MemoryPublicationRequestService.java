package com.ai.repo.service;

import java.time.LocalDateTime;
import com.ai.repo.entity.Memory;

public interface MemoryPublicationRequestService {
    record Created(String requestId, String approvalUrl, Long memoryId, LocalDateTime expiresAt) {}
    record Details(String status, Memory memory, LocalDateTime expiresAt) {}
    record Status(String status, Long memoryId, LocalDateTime expiresAt) {}
    Created create(Long agentId, Long userId, Long memoryId);
    Details details(String requestId, Long userId);
    Status status(String requestId, Long agentId);
    void approve(String requestId, Long userId);
    void reject(String requestId, Long userId);
}
