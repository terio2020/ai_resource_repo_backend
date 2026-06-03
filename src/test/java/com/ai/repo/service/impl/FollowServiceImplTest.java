package com.ai.repo.service.impl;

import com.ai.repo.entity.Follow;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.FollowMapper;
import com.ai.repo.service.AgentService;
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
class FollowServiceImplTest {

    @Mock
    private FollowMapper followMapper;

    @Mock
    private AgentService agentService;

    private FollowServiceImpl followService;

    @BeforeEach
    void setUp() throws Exception {
        followService = new FollowServiceImpl();
        Field mapperField = FollowServiceImpl.class.getDeclaredField("followMapper");
        mapperField.setAccessible(true);
        mapperField.set(followService, followMapper);
        Field agentField = FollowServiceImpl.class.getDeclaredField("agentService");
        agentField.setAccessible(true);
        agentField.set(followService, agentService);
    }

    private Follow createSampleFollow(Long id) {
        Follow f = new Follow();
        f.setId(id);
        f.setFollowerId(1L);
        f.setFollowingId(2L);
        return f;
    }

    // ==================== follow ====================

    @Test
    void follow_shouldSucceed_whenNotAlreadyFollowing() {
        when(followMapper.selectByFollow(1L, 2L)).thenReturn(null);
        when(followMapper.insert(any(Follow.class))).thenReturn(1);

        boolean result = followService.follow(1L, 2L);

        assertTrue(result);
        verify(followMapper).insert(any(Follow.class));
        verify(agentService).incrementFollowingCount(1L, 1);
        verify(agentService).incrementFollowerCount(2L, 1);
    }

    @Test
    void follow_shouldReturnFalse_whenAlreadyFollowing() {
        when(followMapper.selectByFollow(1L, 2L)).thenReturn(createSampleFollow(1L));

        boolean result = followService.follow(1L, 2L);

        assertFalse(result);
        verify(followMapper, never()).insert(any());
        verify(agentService, never()).incrementFollowingCount(anyLong(), anyInt());
        verify(agentService, never()).incrementFollowerCount(anyLong(), anyInt());
    }

    @Test
    void follow_shouldThrow_whenFollowingSelf() {
        BusinessException ex = assertThrows(BusinessException.class, () -> followService.follow(1L, 1L));
        assertTrue(ex.getMessage().contains("Cannot follow yourself"));
        verify(followMapper, never()).insert(any());
        verify(followMapper, never()).selectByFollow(anyLong(), anyLong());
        verify(agentService, never()).incrementFollowingCount(anyLong(), anyInt());
    }

    // ==================== unfollow ====================

    @Test
    void unfollow_shouldSucceed_whenFollowing() {
        when(followMapper.selectByFollow(1L, 2L)).thenReturn(createSampleFollow(1L));
        when(followMapper.deleteByFollow(1L, 2L)).thenReturn(1);

        boolean result = followService.unfollow(1L, 2L);

        assertTrue(result);
        verify(followMapper).deleteByFollow(1L, 2L);
        verify(agentService).incrementFollowingCount(1L, -1);
        verify(agentService).incrementFollowerCount(2L, -1);
    }

    @Test
    void unfollow_shouldReturnFalse_whenNotFollowing() {
        when(followMapper.selectByFollow(1L, 2L)).thenReturn(null);

        boolean result = followService.unfollow(1L, 2L);

        assertFalse(result);
        verify(followMapper, never()).deleteByFollow(anyLong(), anyLong());
        verify(agentService, never()).incrementFollowingCount(anyLong(), anyInt());
        verify(agentService, never()).incrementFollowerCount(anyLong(), anyInt());
    }

    @Test
    void unfollow_shouldReturnFalse_whenDeleteReturnsZero() {
        when(followMapper.selectByFollow(1L, 2L)).thenReturn(createSampleFollow(1L));
        when(followMapper.deleteByFollow(1L, 2L)).thenReturn(0);

        boolean result = followService.unfollow(1L, 2L);

        assertFalse(result);
        verify(followMapper).deleteByFollow(1L, 2L);
        verify(agentService, never()).incrementFollowingCount(anyLong(), anyInt());
        verify(agentService, never()).incrementFollowerCount(anyLong(), anyInt());
    }

    // ==================== isFollowing ====================

    @Test
    void isFollowing_shouldReturnTrue_whenFound() {
        when(followMapper.selectByFollow(1L, 2L)).thenReturn(createSampleFollow(1L));

        boolean result = followService.isFollowing(1L, 2L);

        assertTrue(result);
    }

    @Test
    void isFollowing_shouldReturnFalse_whenNotFound() {
        when(followMapper.selectByFollow(1L, 2L)).thenReturn(null);

        boolean result = followService.isFollowing(1L, 2L);

        assertFalse(result);
    }

    // ==================== findFollowing ====================

    @Test
    void findFollowing_shouldReturnList() {
        when(followMapper.selectByFollowerId(1L)).thenReturn(List.of(createSampleFollow(1L)));

        var result = followService.findFollowing(1L);

        assertEquals(1, result.size());
        verify(followMapper).selectByFollowerId(1L);
    }

    // ==================== findFollowers ====================

    @Test
    void findFollowers_shouldReturnList() {
        when(followMapper.selectByFollowingId(2L)).thenReturn(List.of(createSampleFollow(1L)));

        var result = followService.findFollowers(2L);

        assertEquals(1, result.size());
        verify(followMapper).selectByFollowingId(2L);
    }

    // ==================== countFollowing ====================

    @Test
    void countFollowing_shouldReturnCount() {
        when(followMapper.countFollowing(1L)).thenReturn(5L);

        Long result = followService.countFollowing(1L);

        assertEquals(5L, result);
        verify(followMapper).countFollowing(1L);
    }

    // ==================== countFollowers ====================

    @Test
    void countFollowers_shouldReturnCount() {
        when(followMapper.countFollowers(2L)).thenReturn(10L);

        Long result = followService.countFollowers(2L);

        assertEquals(10L, result);
        verify(followMapper).countFollowers(2L);
    }
}
