package com.ai.repo.mapper;

import com.ai.repo.entity.UserChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserChatMessageMapper {
    UserChatMessage findById(String id);
    List<UserChatMessage> findBySenderId(String senderId);
    List<UserChatMessage> findByReceiverId(String receiverId);
    List<UserChatMessage> findConversation(String userId1, String userId2);
    List<UserChatMessage> findAll();
    int insert(UserChatMessage message);
    int update(UserChatMessage message);
    int deleteById(String id);
}
