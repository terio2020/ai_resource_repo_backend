package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileMemoryItemHistory {
    private Long id;
    private String uid;
    private Long itemId;
    private Long userId;
    private String action;
    private String previousValueJson;
    private String newValueJson;
    private String previousStatus;
    private String newStatus;
    private String note;
    private LocalDateTime createdAt;
}
