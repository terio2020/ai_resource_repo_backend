package com.ai.repo.service.impl;

import com.ai.repo.entity.ChatMessage;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.ChatMessageMapper;
import com.ai.repo.service.ChatMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public ChatMessage create(ChatMessage message) {
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public boolean delete(Long id) {
        if (chatMessageMapper.selectById(id) == null) {
            throw new BusinessException("Chat message not found");
        }
        return chatMessageMapper.deleteById(id) > 0;
    }

    @Override
    public ChatMessage findById(Long id) {
        return chatMessageMapper.selectById(id);
    }

    @Override
    public List<ChatMessage> findAll() {
        return chatMessageMapper.selectAll();
    }

    @Override
    public List<ChatMessage> findByRoomId(String roomId) {
        return chatMessageMapper.selectByRoomId(roomId);
    }

    @Override
    public List<ChatMessage> findBySenderId(Long senderId) {
        return chatMessageMapper.selectBySenderId(senderId);
    }

    @Override
    public List<ChatMessage> findRecentMessages(String roomId, int limit) {
        return chatMessageMapper.selectByRoomIdAndLimit(roomId, limit);
    }
}
