package com.ai.repo.service.impl;

import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.MemoryMapper;
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
class MemoryServiceImplTest {

    @Mock
    private MemoryMapper memoryMapper;

    private MemoryServiceImpl memoryService;

    @BeforeEach
    void setUp() throws Exception {
        memoryService = new MemoryServiceImpl();
        Field field = MemoryServiceImpl.class.getDeclaredField("memoryMapper");
        field.setAccessible(true);
        field.set(memoryService, memoryMapper);
    }

    private Memory createSampleMemory(Long id) {
        Memory m = new Memory();
        m.setId(id);
        m.setUserId(1L);
        m.setAgentId(1L);
        m.setTitle("test-memory");
        m.setContent("test content");
        m.setCategory("general");
        m.setIsPublic(true);
        return m;
    }

    // ==================== create ====================

    @Test
    void create_shouldInsertAndReturn() {
        Memory memory = createSampleMemory(null);
        when(memoryMapper.insert(memory)).thenReturn(1);

        Memory result = memoryService.create(memory);

        assertNotNull(result);
        verify(memoryMapper).insert(memory);
    }

    // ==================== update ====================

    @Test
    void update_shouldUpdate_whenExists() {
        Memory memory = createSampleMemory(1L);
        when(memoryMapper.selectById(1L)).thenReturn(memory);
        when(memoryMapper.update(memory)).thenReturn(1);

        Memory result = memoryService.update(memory);

        assertNotNull(result);
        verify(memoryMapper).update(memory);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        Memory memory = createSampleMemory(999L);
        when(memoryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> memoryService.update(memory));
        assertTrue(ex.getMessage().contains("Memory not found"));
        verify(memoryMapper, never()).update(any());
    }

    // ==================== delete ====================

    @Test
    void delete_shouldDelete_whenExists() {
        when(memoryMapper.selectById(1L)).thenReturn(createSampleMemory(1L));
        when(memoryMapper.deleteById(1L)).thenReturn(1);

        boolean result = memoryService.delete(1L);

        assertTrue(result);
        verify(memoryMapper).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(memoryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> memoryService.delete(999L));
        assertTrue(ex.getMessage().contains("Memory not found"));
        verify(memoryMapper, never()).deleteById(any());
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturnMemory() {
        Memory memory = createSampleMemory(1L);
        when(memoryMapper.selectById(1L)).thenReturn(memory);

        Memory result = memoryService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldReturnNull_whenNotFound() {
        when(memoryMapper.selectById(999L)).thenReturn(null);

        Memory result = memoryService.findById(999L);

        assertNull(result);
    }

    // ==================== findAll ====================

    @Test
    void findAll_shouldReturnList() {
        when(memoryMapper.selectAll()).thenReturn(List.of(createSampleMemory(1L)));

        var result = memoryService.findAll();

        assertEquals(1, result.size());
        verify(memoryMapper).selectAll();
    }

    // ==================== findByUserId ====================

    @Test
    void findByUserId_shouldReturnList() {
        when(memoryMapper.selectByUserId(1L)).thenReturn(List.of(createSampleMemory(1L)));

        var result = memoryService.findByUserId(1L);

        assertEquals(1, result.size());
        verify(memoryMapper).selectByUserId(1L);
    }

    // ==================== findByAgentId ====================

    @Test
    void findByAgentId_shouldReturnList() {
        when(memoryMapper.selectByAgentId(1L)).thenReturn(List.of(createSampleMemory(1L)));

        var result = memoryService.findByAgentId(1L);

        assertEquals(1, result.size());
        verify(memoryMapper).selectByAgentId(1L);
    }

    // ==================== findByCategory ====================

    @Test
    void findByCategory_shouldReturnList() {
        when(memoryMapper.selectByCategory("general")).thenReturn(List.of(createSampleMemory(1L)));

        var result = memoryService.findByCategory("general");

        assertEquals(1, result.size());
        verify(memoryMapper).selectByCategory("general");
    }

    // ==================== findByPublic ====================

    @Test
    void findByPublic_shouldReturnList() {
        when(memoryMapper.selectByPublic(true)).thenReturn(List.of(createSampleMemory(1L)));

        var result = memoryService.findByPublic(true);

        assertEquals(1, result.size());
        verify(memoryMapper).selectByPublic(true);
    }

    // ==================== searchByKeyword ====================

    @Test
    void searchByKeyword_shouldReturnList() {
        when(memoryMapper.searchByKeyword("test")).thenReturn(List.of(createSampleMemory(1L)));

        var result = memoryService.searchByKeyword("test");

        assertEquals(1, result.size());
        verify(memoryMapper).searchByKeyword("test");
    }

    // ==================== batchDelete ====================

    @Test
    void batchDelete_shouldDelete_whenValidIds() {
        when(memoryMapper.batchDelete(List.of(1L, 2L))).thenReturn(2);

        int result = memoryService.batchDelete(List.of(1L, 2L));

        assertEquals(2, result);
        verify(memoryMapper).batchDelete(List.of(1L, 2L));
    }

    @Test
    void batchDelete_shouldThrow_whenNullIds() {
        BusinessException ex = assertThrows(BusinessException.class, () -> memoryService.batchDelete(null));
        assertTrue(ex.getMessage().contains("IDs cannot be null or empty"));
        verify(memoryMapper, never()).batchDelete(any());
    }

    @Test
    void batchDelete_shouldThrow_whenEmptyIds() {
        BusinessException ex = assertThrows(BusinessException.class, () -> memoryService.batchDelete(List.of()));
        assertTrue(ex.getMessage().contains("IDs cannot be null or empty"));
        verify(memoryMapper, never()).batchDelete(any());
    }

    // ==================== upsert ====================

    @Test
    void upsert_shouldUpdate_whenExistingFound() {
        Memory memory = createSampleMemory(null);
        Memory existing = createSampleMemory(1L);
        when(memoryMapper.selectByUserIdAndAgentIdAndTitle(1L, 1L, "test-memory")).thenReturn(existing);

        Memory result = memoryService.upsert(memory);

        assertEquals(1L, result.getId());
        verify(memoryMapper).updateByCompositeKey(memory);
        verify(memoryMapper, never()).insert(any());
    }

    @Test
    void upsert_shouldInsert_whenNoExisting() {
        Memory memory = createSampleMemory(null);
        when(memoryMapper.selectByUserIdAndAgentIdAndTitle(1L, 1L, "test-memory")).thenReturn(null);

        Memory result = memoryService.upsert(memory);

        assertNull(result.getId());
        verify(memoryMapper).insert(memory);
        verify(memoryMapper, never()).updateByCompositeKey(any());
    }

    // ==================== incrementDownloadCount ====================

    @Test
    void incrementDownloadCount_shouldSucceed() {
        when(memoryMapper.selectById(1L)).thenReturn(createSampleMemory(1L));
        when(memoryMapper.incrementDownloadCount(1L)).thenReturn(1);

        boolean result = memoryService.incrementDownloadCount(1L);

        assertTrue(result);
        verify(memoryMapper).incrementDownloadCount(1L);
    }

    @Test
    void incrementDownloadCount_shouldThrow_whenNotFound() {
        when(memoryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> memoryService.incrementDownloadCount(999L));
        assertTrue(ex.getMessage().contains("Memory not found"));
        verify(memoryMapper, never()).incrementDownloadCount(any());
    }

    // ==================== incrementLikeCount ====================

    @Test
    void incrementLikeCount_shouldSucceed() {
        when(memoryMapper.selectById(1L)).thenReturn(createSampleMemory(1L));
        when(memoryMapper.incrementLikeCount(1L)).thenReturn(1);

        boolean result = memoryService.incrementLikeCount(1L);

        assertTrue(result);
        verify(memoryMapper).incrementLikeCount(1L);
    }

    @Test
    void incrementLikeCount_shouldThrow_whenNotFound() {
        when(memoryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> memoryService.incrementLikeCount(999L));
        assertTrue(ex.getMessage().contains("Memory not found"));
        verify(memoryMapper, never()).incrementLikeCount(any());
    }
}
