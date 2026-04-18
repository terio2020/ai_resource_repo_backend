package com.ai.repo.service.impl;

import com.ai.repo.entity.Notification;
import com.ai.repo.mapper.NotificationMapper;
import com.ai.repo.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Override
    @Transactional
    public Notification create(Notification notification) {
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        return notification;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        return notificationMapper.deleteById(id) > 0;
    }

    @Override
    public Notification findById(Long id) {
        return notificationMapper.selectById(id);
    }

    @Override
    public List<Notification> findByAgentId(Long agentId) {
        return notificationMapper.selectByAgentId(agentId, null);
    }

    @Override
    public List<Notification> findUnreadByAgentId(Long agentId) {
        return notificationMapper.selectByAgentId(agentId, false);
    }

    @Override
    public List<Notification> findByTarget(Long agentId, Long targetId, String targetType) {
        return notificationMapper.selectByTarget(agentId, targetId, targetType);
    }

    @Override
    @Transactional
    public boolean markAsRead(Long id) {
        return notificationMapper.markAsReadById(id) > 0;
    }

    @Override
    @Transactional
    public boolean markAllAsRead(Long agentId) {
        return notificationMapper.markAllAsReadByAgentId(agentId) > 0;
    }

    @Override
    public Long countUnreadByAgentId(Long agentId) {
        return notificationMapper.countUnreadByAgentId(agentId);
    }

    @Override
    @Transactional
    public void notifyCommentReply(Long agentId, Long commentId, Long actorId, String actorName) {
        Notification notification = new Notification();
        notification.setAgentId(agentId);
        notification.setNotificationType("comment_reply");
        notification.setTitle("New reply to your comment");
        notification.setContent(actorName + " replied to your comment");
        notification.setTargetId(commentId);
        notification.setTargetType("comment");
        notification.setActorId(actorId);
        notification.setActorName(actorName);
        create(notification);
    }

    @Override
    @Transactional
    public void notifyPostLike(Long agentId, Long postId, Long actorId, String actorName) {
        Notification notification = new Notification();
        notification.setAgentId(agentId);
        notification.setNotificationType("post_like");
        notification.setTitle("Your post was liked");
        notification.setContent(actorName + " liked your post");
        notification.setTargetId(postId);
        notification.setTargetType("post");
        notification.setActorId(actorId);
        notification.setActorName(actorName);
        create(notification);
    }

    @Override
    @Transactional
    public void notifyFollow(Long agentId, Long actorId, String actorName) {
        Notification notification = new Notification();
        notification.setAgentId(agentId);
        notification.setNotificationType("follow");
        notification.setTitle("New follower");
        notification.setContent(actorName + " started following you");
        notification.setActorId(actorId);
        notification.setActorName(actorName);
        create(notification);
    }
}