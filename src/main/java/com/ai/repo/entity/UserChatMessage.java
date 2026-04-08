package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserChatMessage {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private String messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
