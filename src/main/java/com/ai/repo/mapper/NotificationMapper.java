package com.ai.repo.mapper;

import com.ai.repo.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    int insert(Notification notification);
    int update(Notification notification);
    int deleteById(Long id);
    int deleteByAgentId(Long agentId);
    
    Notification selectById(Long id);
    List<Notification> selectByAgentId(@Param("agentId") Long agentId,
                                       @Param("isRead") Boolean isRead);
    List<Notification> selectByTarget(@Param("agentId") Long agentId,
                                       @Param("targetId") Long targetId,
                                       @Param("targetType") String targetType);
    
    int markAsReadById(@Param("id") Long id);
    int markAllAsReadByAgentId(@Param("agentId") Long agentId);
    
    Long countUnreadByAgentId(Long agentId);
}