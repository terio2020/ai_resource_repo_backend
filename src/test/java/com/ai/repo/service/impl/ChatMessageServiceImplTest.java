package com.ai.repo.service.impl;

import com.ai.repo.entity.ChatMessage;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.ChatMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    private ChatMessageServiceImpl chatMessageService;

    @BeforeEach
    void setUp() throws Exception {
        chatMessageService = new ChatMessageServiceImpl();
        Field field = ChatMessageServiceImpl.class.getDeclaredField("chatMessageMapper");
        field.setAccessible(true);
        field.set(chatMessageService, chatMessageMapper);
    }

    private ChatMessage createSampleMessage(Long id) {
        return new ChatMessage(
                id,
                1L,
                "user",
                "test-user",
                "hello world",
                "text",
                "room-1",
                false,
                null
        );
    }

    // ==================== create ====================

    @Test
    void create_shouldInsertAndReturn() {
        ChatMessage message = createSampleMessage(null);
        when(chatMessageMapper.insert(message)).thenReturn(1);

        ChatMessage result = chatMessageService.create(message);

        assertNotNull(result);
        verify(chatMessageMapper).insert(message);
    }

    // ==================== delete ====================

    @Test
    void delete_shouldDelete_whenExists() {
        when(chatMessageMapper.selectById(1L)).thenReturn(createSampleMessage(1L));
        when(chatMessageMapper.deleteById(1L)).thenReturn(1);

        boolean result = chatMessageService.delete(1L);

        assertTrue(result);
        verify(chatMessageMapper).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(chatMessageMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> chatMessageService.delete(999L));
        assertTrue(ex.getMessage().contains("Chat message not found"));
        verify(chatMessageMapper, never()).deleteById(any());
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturnMessage() {
        ChatMessage message = createSampleMessage(1L);
        when(chatMessageMapper.selectById(1L)).thenReturn(message);

        ChatMessage result = chatMessageService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldReturnNull_whenNotFound() {
        when(chatMessageMapper.selectById(999L)).thenReturn(null);

        ChatMessage result = chatMessageService.findById(999L);

        assertNull(result);
    }

    // ==================== findAll ====================

    @Test
    void findAll_shouldReturnList() {
        when(chatMessageMapper.selectAll()).thenReturn(List.of(createSampleMessage(1L)));

        var result = chatMessageService.findAll();

        assertEquals(1, result.size());
        verify(chatMessageMapper).selectAll();
    }

    // ==================== findByRoomId ====================

    @Test
    void findByRoomId_shouldReturnList() {
        when(chatMessageMapper.selectByRoomId("room-1")).thenReturn(List.of(createSampleMessage(1L)));

        var result = chatMessageService.findByRoomId("room-1");

        assertEquals(1, result.size());
        verify(chatMessageMapper).selectByRoomId("room-1");
    }

    // ==================== findBySenderId ====================

    @Test
    void findBySenderId_shouldReturnList() {
        when(chatMessageMapper.selectBySenderId(1L)).thenReturn(List.of(createSampleMessage(1L)));

        var result = chatMessageService.findBySenderId(1L);

        assertEquals(1, result.size());
        verify(chatMessageMapper).selectBySenderId(1L);
    }

    // ==================== findRecentMessages ====================

    @Test
    void findRecentMessages_shouldReturnList() {
        when(chatMessageMapper.selectByRoomIdAndLimit("room-1", 10)).thenReturn(List.of(createSampleMessage(1L)));

        var result = chatMessageService.findRecentMessages("room-1", 10);

        assertEquals(1, result.size());
        verify(chatMessageMapper).selectByRoomIdAndLimit("room-1", 10);
    }
}
