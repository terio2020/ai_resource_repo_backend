package com.ai.repo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProfileMemoryItem {
    private Long id;
    private String uid;
    private Long memoryId;
    private Long userId;
    private Long sourceAgentId;
    private String itemKey;
    private String namespace;
    private String factKey;
    private String valueType;
    private String valueJson;
    private String valueHash;
    private String contextJson;
    private String contextHash;
    private String recordType;
    private BigDecimal confidence;
    private String sensitivity;
    private String status;
    private LocalDateTime observedAt;
    private LocalDateTime validUntil;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
