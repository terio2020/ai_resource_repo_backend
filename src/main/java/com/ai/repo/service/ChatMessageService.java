package com.ai.repo.service;

import com.ai.repo.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    ChatMessage create(ChatMessage message);
    boolean delete(Long id);
    ChatMessage findById(Long id);
    List<ChatMessage> findAll();
    List<ChatMessage> findByRoomId(String roomId);
    List<ChatMessage> findBySenderId(Long senderId);
    List<ChatMessage> findRecentMessages(String roomId, int limit);
}
