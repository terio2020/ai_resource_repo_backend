package com.ai.repo.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MemoryPublicationRequest {
    private Long id;
    private String tokenHash;
    private Long userId;
    private Long agentId;
    private Long memoryId;
    private String contentHash;
    private String status;
    private LocalDateTime expiresAt;
}
