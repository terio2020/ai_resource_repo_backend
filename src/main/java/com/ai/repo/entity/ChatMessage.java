package com.ai.repo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long id;
    private Long senderId;
    private String senderType;
    private String senderName;
    private String content;
    private String messageType;
    private String roomId;
    private Boolean isSystem;
    private LocalDateTime createdAt;
}
