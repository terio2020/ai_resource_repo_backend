package com.ai.repo.service.impl;

import com.ai.repo.entity.Notification;
import com.ai.repo.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() throws Exception {
        notificationService = new NotificationServiceImpl();
        Field field = NotificationServiceImpl.class.getDeclaredField("notificationMapper");
        field.setAccessible(true);
        field.set(notificationService, notificationMapper);
    }

    private Notification createSampleNotification(Long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setAgentId(1L);
        n.setNotificationType("test_type");
        n.setTitle("Test Title");
        n.setContent("Test Content");
        n.setTargetId(100L);
        n.setTargetType("comment");
        n.setActorId(2L);
        n.setActorName("actor");
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    // ==================== create ====================

    @Test
    void create_shouldSetDefaultsAndInsert() {
        Notification notification = new Notification();
        notification.setAgentId(1L);
        notification.setNotificationType("test");
        notification.setTitle("Title");
        notification.setContent("Content");

        Notification result = notificationService.create(notification);

        assertNotNull(result);
        assertFalse(result.getIsRead());
        assertNotNull(result.getCreatedAt());
        verify(notificationMapper).insert(notification);
    }

    // ==================== delete ====================

    @Test
    void delete_shouldReturnTrue_whenDeleted() {
        when(notificationMapper.deleteById(1L)).thenReturn(1);

        boolean result = notificationService.delete(1L);

        assertTrue(result);
        verify(notificationMapper).deleteById(1L);
    }

    @Test
    void delete_shouldReturnFalse_whenNotFound() {
        when(notificationMapper.deleteById(999L)).thenReturn(0);

        boolean result = notificationService.delete(999L);

        assertFalse(result);
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturnNotification() {
        Notification notification = createSampleNotification(1L);
        when(notificationMapper.selectById(1L)).thenReturn(notification);

        Notification result = notificationService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldReturnNull_whenNotFound() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        Notification result = notificationService.findById(999L);

        assertNull(result);
    }

    // ==================== findByAgentId ====================

    @Test
    void findByAgentId_shouldReturnList() {
        when(notificationMapper.selectByAgentId(1L, null)).thenReturn(List.of(createSampleNotification(1L)));

        var result = notificationService.findByAgentId(1L);

        assertEquals(1, result.size());
        verify(notificationMapper).selectByAgentId(1L, null);
    }

    // ==================== findUnreadByAgentId ====================

    @Test
    void findUnreadByAgentId_shouldReturnUnreadList() {
        when(notificationMapper.selectByAgentId(1L, false)).thenReturn(List.of(createSampleNotification(1L)));

        var result = notificationService.findUnreadByAgentId(1L);

        assertEquals(1, result.size());
        verify(notificationMapper).selectByAgentId(1L, false);
    }

    // ==================== findByTarget ====================

    @Test
    void findByTarget_shouldReturnList() {
        when(notificationMapper.selectByTarget(1L, 100L, "comment")).thenReturn(List.of(createSampleNotification(1L)));

        var result = notificationService.findByTarget(1L, 100L, "comment");

        assertEquals(1, result.size());
        verify(notificationMapper).selectByTarget(1L, 100L, "comment");
    }

    // ==================== markAsRead ====================

    @Test
    void markAsRead_shouldReturnTrue_whenUpdated() {
        when(notificationMapper.markAsReadById(1L)).thenReturn(1);

        boolean result = notificationService.markAsRead(1L);

        assertTrue(result);
        verify(notificationMapper).markAsReadById(1L);
    }

    @Test
    void markAsRead_shouldReturnFalse_whenNotFound() {
        when(notificationMapper.markAsReadById(999L)).thenReturn(0);

        boolean result = notificationService.markAsRead(999L);

        assertFalse(result);
    }

    // ==================== markAllAsRead ====================

    @Test
    void markAllAsRead_shouldReturnTrue_whenUpdated() {
        when(notificationMapper.markAllAsReadByAgentId(1L)).thenReturn(3);

        boolean result = notificationService.markAllAsRead(1L);

        assertTrue(result);
        verify(notificationMapper).markAllAsReadByAgentId(1L);
    }

    @Test
    void markAllAsRead_shouldReturnFalse_whenNoUnread() {
        when(notificationMapper.markAllAsReadByAgentId(1L)).thenReturn(0);

        boolean result = notificationService.markAllAsRead(1L);

        assertFalse(result);
    }

    // ==================== countUnreadByAgentId ====================

    @Test
    void countUnreadByAgentId_shouldReturnCount() {
        when(notificationMapper.countUnreadByAgentId(1L)).thenReturn(5L);

        Long result = notificationService.countUnreadByAgentId(1L);

        assertEquals(5L, result);
    }

    @Test
    void countUnreadByAgentId_shouldReturnZero_whenNoUnread() {
        when(notificationMapper.countUnreadByAgentId(1L)).thenReturn(0L);

        Long result = notificationService.countUnreadByAgentId(1L);

        assertEquals(0L, result);
    }

    // ==================== notifyCommentReply ====================

    @Test
    void notifyCommentReply_shouldCreateNotification() {
        notificationService.notifyCommentReply(1L, 100L, 2L, "actor");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());

        Notification n = captor.getValue();
        assertEquals(1L, n.getAgentId());
        assertEquals("comment_reply", n.getNotificationType());
        assertEquals("New reply to your comment", n.getTitle());
        assertEquals("actor replied to your comment", n.getContent());
        assertEquals(100L, n.getTargetId());
        assertEquals("comment", n.getTargetType());
        assertEquals(2L, n.getActorId());
        assertEquals("actor", n.getActorName());
        assertFalse(n.getIsRead());
        assertNotNull(n.getCreatedAt());
    }

    // ==================== notifyPostLike ====================

    @Test
    void notifyPostLike_shouldCreateNotification() {
        notificationService.notifyPostLike(1L, 200L, 2L, "actor");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());

        Notification n = captor.getValue();
        assertEquals(1L, n.getAgentId());
        assertEquals("post_like", n.getNotificationType());
        assertEquals("Your post was liked", n.getTitle());
        assertEquals("actor liked your post", n.getContent());
        assertEquals(200L, n.getTargetId());
        assertEquals("post", n.getTargetType());
        assertEquals(2L, n.getActorId());
        assertEquals("actor", n.getActorName());
    }

    // ==================== notifyFollow ====================

    @Test
    void notifyFollow_shouldCreateNotification() {
        notificationService.notifyFollow(1L, 2L, "actor");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());

        Notification n = captor.getValue();
        assertEquals(1L, n.getAgentId());
        assertEquals("follow", n.getNotificationType());
        assertEquals("New follower", n.getTitle());
        assertEquals("actor started following you", n.getContent());
        assertEquals(2L, n.getActorId());
        assertEquals("actor", n.getActorName());
    }
}
