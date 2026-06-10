package com.ai.repo.service.impl;

import com.ai.repo.entity.Statistics;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.StatisticsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private StatisticsMapper statisticsMapper;

    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() throws Exception {
        statisticsService = new StatisticsServiceImpl();
        Field field = StatisticsServiceImpl.class.getDeclaredField("statisticsMapper");
        field.setAccessible(true);
        field.set(statisticsService, statisticsMapper);
    }

    private Statistics sampleStats(Long id) {
        Statistics s = new Statistics();
        s.setId(id);
        s.setUserId(1L);
        s.setMetricType("skill_count");
        s.setMetricName("Skills");
        s.setMetricValue(BigDecimal.valueOf(5));
        s.setUnit("count");
        s.setDate(LocalDate.now());
        return s;
    }

    @Test
    void create_shouldInsertAndReturn() {
        Statistics stats = sampleStats(null);
        when(statisticsMapper.insert(stats)).thenReturn(1);

        Statistics result = statisticsService.create(stats);

        assertNotNull(result);
        verify(statisticsMapper).insert(stats);
    }

    @Test
    void delete_shouldDelete_whenExists() {
        when(statisticsMapper.selectById(1L)).thenReturn(sampleStats(1L));
        when(statisticsMapper.deleteById(1L)).thenReturn(1);

        boolean result = statisticsService.delete(1L);

        assertTrue(result);
        verify(statisticsMapper).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(statisticsMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> statisticsService.delete(999L));
        assertTrue(ex.getMessage().contains("Statistics not found"));
        verify(statisticsMapper, never()).deleteById(any());
    }

    @Test
    void findById_shouldReturnStats() {
        when(statisticsMapper.selectById(1L)).thenReturn(sampleStats(1L));

        Statistics result = statisticsService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldReturnNull_whenNotFound() {
        when(statisticsMapper.selectById(999L)).thenReturn(null);
        assertNull(statisticsService.findById(999L));
    }

    @Test
    void findAll_shouldReturnList() {
        when(statisticsMapper.selectAll()).thenReturn(List.of(sampleStats(1L)));
        assertEquals(1, statisticsService.findAll().size());
    }

    @Test
    void findByUserId_shouldReturnList() {
        when(statisticsMapper.selectByUserId(1L)).thenReturn(List.of(sampleStats(1L)));
        assertEquals(1, statisticsService.findByUserId(1L).size());
    }

    @Test
    void findByUserIdAndDateRange_shouldReturnList() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        when(statisticsMapper.selectByUserIdAndDateRange(1L, start, end)).thenReturn(List.of(sampleStats(1L)));

        var result = statisticsService.findByUserIdAndDateRange(1L, start, end);

        assertEquals(1, result.size());
        verify(statisticsMapper).selectByUserIdAndDateRange(1L, start, end);
    }

    @Test
    void findByMetricType_shouldReturnList() {
        when(statisticsMapper.selectByMetricType("skill_count")).thenReturn(List.of(sampleStats(1L)));
        assertEquals(1, statisticsService.findByMetricType("skill_count").size());
    }

    @Test
    void findByUserIdAndMetricType_shouldReturnList() {
        when(statisticsMapper.selectByUserIdAndMetricType(1L, "skill_count")).thenReturn(List.of(sampleStats(1L)));
        assertEquals(1, statisticsService.findByUserIdAndMetricType(1L, "skill_count").size());
    }
}
