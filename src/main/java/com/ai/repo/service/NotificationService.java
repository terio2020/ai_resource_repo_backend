package com.ai.repo.service;

import com.ai.repo.entity.Notification;

import java.util.List;

public interface NotificationService {
    Notification create(Notification notification);
    boolean delete(Long id);
    Notification findById(Long id);
    List<Notification> findByAgentId(Long agentId);
    List<Notification> findUnreadByAgentId(Long agentId);
    List<Notification> findByTarget(Long agentId, Long targetId, String targetType);
    boolean markAsRead(Long id);
    boolean markAllAsRead(Long agentId);
    Long countUnreadByAgentId(Long agentId);
    
    void notifyCommentReply(Long agentId, Long commentId, Long actorId, String actorName);
    void notifyPostLike(Long agentId, Long postId, Long actorId, String actorName);
    void notifyFollow(Long agentId, Long actorId, String actorName);
}