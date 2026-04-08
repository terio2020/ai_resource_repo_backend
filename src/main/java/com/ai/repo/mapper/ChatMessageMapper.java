package com.ai.repo.mapper;

import com.ai.repo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    int insert(ChatMessage message);
    int deleteById(Long id);
    ChatMessage selectById(Long id);
    List<ChatMessage> selectAll();
    List<ChatMessage> selectByRoomId(String roomId);
    List<ChatMessage> selectBySenderId(Long senderId);
    List<ChatMessage> selectByRoomIdAndLimit(String roomId, int limit);
}
