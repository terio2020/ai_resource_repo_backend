package com.ai.repo.service.impl;

import com.ai.repo.entity.Skill;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.SkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillMapper skillMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private SkillServiceImpl skillService;

    @BeforeEach
    void setUp() throws Exception {
        skillService = new SkillServiceImpl();
        java.lang.reflect.Field field = SkillServiceImpl.class.getDeclaredField("skillMapper");
        field.setAccessible(true);
        field.set(skillService, skillMapper);
        field = SkillServiceImpl.class.getDeclaredField("redisTemplate");
        field.setAccessible(true);
        field.set(skillService, redisTemplate);
    }

    private Skill createSampleSkill(Long id) {
        Skill s = new Skill();
        s.setId(id);
        s.setUserId(1L);
        s.setAgentId(1L);
        s.setName("test-skill");
        s.setVersion("1.0");
        s.setCategory("java");
        s.setIsPublic(true);
        return s;
    }

    // ==================== create ====================

    @Test
    void create_shouldInsertAndReturn() {
        Skill skill = createSampleSkill(null);
        when(skillMapper.insert(skill)).thenReturn(1);

        Skill result = skillService.create(skill);

        assertNotNull(result);
        verify(skillMapper).insert(skill);
    }

    // ==================== update ====================

    @Test
    void update_shouldUpdate_whenExists() {
        Skill skill = createSampleSkill(1L);
        when(skillMapper.selectById(1L)).thenReturn(skill);
        when(skillMapper.update(skill)).thenReturn(1);

        Skill result = skillService.update(skill);

        assertNotNull(result);
        verify(skillMapper).update(skill);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        Skill skill = createSampleSkill(999L);
        when(skillMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> skillService.update(skill));
        assertTrue(ex.getMessage().contains("Skill not found"));
        verify(skillMapper, never()).update(any());
    }

    // ==================== delete ====================

    @Test
    void delete_shouldDelete_whenExists() {
        when(skillMapper.selectById(1L)).thenReturn(createSampleSkill(1L));
        when(skillMapper.deleteById(1L)).thenReturn(1);

        boolean result = skillService.delete(1L);

        assertTrue(result);
        verify(skillMapper).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(skillMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> skillService.delete(999L));
        assertTrue(ex.getMessage().contains("Skill not found"));
        verify(skillMapper, never()).deleteById(any());
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturnSkill() {
        Skill skill = createSampleSkill(1L);
        when(skillMapper.selectById(1L)).thenReturn(skill);

        Skill result = skillService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldReturnNull_whenNotFound() {
        when(skillMapper.selectById(999L)).thenReturn(null);

        Skill result = skillService.findById(999L);

        assertNull(result);
    }

    // ==================== findAll ====================

    @Test
    void findAll_shouldReturnList() {
        when(skillMapper.selectAll()).thenReturn(java.util.List.of(createSampleSkill(1L)));

        var result = skillService.findAll();

        assertEquals(1, result.size());
        verify(skillMapper).selectAll();
    }

    // ==================== findByUserId ====================

    @Test
    void findByUserId_shouldReturnList() {
        when(skillMapper.selectByUserId(1L)).thenReturn(java.util.List.of(createSampleSkill(1L)));

        var result = skillService.findByUserId(1L);

        assertEquals(1, result.size());
        verify(skillMapper).selectByUserId(1L);
    }

    // ==================== findByAgentId ====================

    @Test
    void findByAgentId_shouldReturnList() {
        when(skillMapper.selectByAgentId(1L)).thenReturn(java.util.List.of(createSampleSkill(1L)));

        var result = skillService.findByAgentId(1L);

        assertEquals(1, result.size());
        verify(skillMapper).selectByAgentId(1L);
    }

    // ==================== findByCategory ====================

    @Test
    void findByCategory_shouldReturnList() {
        when(skillMapper.selectByCategory("java")).thenReturn(java.util.List.of(createSampleSkill(1L)));

        var result = skillService.findByCategory("java");

        assertEquals(1, result.size());
        verify(skillMapper).selectByCategory("java");
    }

    // ==================== findByPublic ====================

    @Test
    void findByPublic_shouldReturnList() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:skills:public")).thenReturn(null);
        when(skillMapper.selectByPublic(true)).thenReturn(java.util.List.of(createSampleSkill(1L)));

        var result = skillService.findByPublic(true);

        assertEquals(1, result.size());
        verify(skillMapper).selectByPublic(true);
        verify(valueOperations).set(eq("cache:skills:public"), any(), eq(60L), eq(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void findByPublic_shouldReturnCached() {
        var cached = java.util.List.of(createSampleSkill(2L));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:skills:public")).thenReturn(cached);

        var result = skillService.findByPublic(true);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(2L), result.get(0).getId());
        verify(skillMapper, never()).selectByPublic(any());
    }

    // ==================== searchByKeyword ====================

    @Test
    void searchByKeyword_shouldReturnList() {
        when(skillMapper.searchByKeyword("test")).thenReturn(java.util.List.of(createSampleSkill(1L)));

        var result = skillService.searchByKeyword("test");

        assertEquals(1, result.size());
        verify(skillMapper).searchByKeyword("test");
    }

    // ==================== incrementDownloadCount ====================

    @Test
    void incrementDownloadCount_shouldSucceed() {
        when(skillMapper.incrementDownloadCount(1L)).thenReturn(1);

        boolean result = skillService.incrementDownloadCount(1L);

        assertTrue(result);
        verify(skillMapper).incrementDownloadCount(1L);
    }

    @Test
    void incrementDownloadCount_shouldThrow_whenNotFound() {
        when(skillMapper.incrementDownloadCount(999L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> skillService.incrementDownloadCount(999L));
        assertTrue(ex.getMessage().contains("Skill not found"));
    }

    // ==================== incrementLikeCount ====================

    @Test
    void incrementLikeCount_shouldSucceed() {
        when(skillMapper.incrementLikeCount(1L)).thenReturn(1);

        boolean result = skillService.incrementLikeCount(1L);

        assertTrue(result);
        verify(skillMapper).incrementLikeCount(1L);
    }

    @Test
    void incrementLikeCount_shouldThrow_whenNotFound() {
        when(skillMapper.incrementLikeCount(999L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> skillService.incrementLikeCount(999L));
        assertTrue(ex.getMessage().contains("Skill not found"));
    }

    // ==================== batchDelete ====================

    @Test
    void batchDelete_shouldDelete_whenValidIds() {
        when(skillMapper.batchDelete(java.util.List.of(1L, 2L))).thenReturn(2);

        int result = skillService.batchDelete(java.util.List.of(1L, 2L));

        assertEquals(2, result);
        verify(skillMapper).batchDelete(java.util.List.of(1L, 2L));
    }

    @Test
    void batchDelete_shouldThrow_whenNullIds() {
        BusinessException ex = assertThrows(BusinessException.class, () -> skillService.batchDelete(null));
        assertTrue(ex.getMessage().contains("IDs cannot be null or empty"));
        verify(skillMapper, never()).batchDelete(any());
    }

    @Test
    void batchDelete_shouldThrow_whenEmptyIds() {
        BusinessException ex = assertThrows(BusinessException.class, () -> skillService.batchDelete(java.util.List.of()));
        assertTrue(ex.getMessage().contains("IDs cannot be null or empty"));
        verify(skillMapper, never()).batchDelete(any());
    }

    // ==================== upsert ====================

    @Test
    void upsert_shouldUpdate_whenExistingFound() {
        Skill skill = createSampleSkill(null);
        Skill existing = createSampleSkill(1L);
        when(skillMapper.selectByUserIdAndAgentIdAndName(1L, 1L, "test-skill")).thenReturn(existing);

        Skill result = skillService.upsert(skill);

        assertEquals(1L, result.getId());
        verify(skillMapper).updateByCompositeKey(skill);
        verify(skillMapper, never()).insert(any());
    }

    @Test
    void upsert_shouldInsert_whenNoExisting() {
        Skill skill = createSampleSkill(null);
        when(skillMapper.selectByUserIdAndAgentIdAndName(1L, 1L, "test-skill")).thenReturn(null);

        Skill result = skillService.upsert(skill);

        assertNull(result.getId());
        verify(skillMapper).insert(skill);
        verify(skillMapper, never()).updateByCompositeKey(any());
    }
}
