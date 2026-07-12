package com.ai.repo.service.impl;

import com.ai.repo.dto.RepoRatingAverageResponse;
import com.ai.repo.dto.RepoRatingRequest;
import com.ai.repo.dto.RepoRatingResponse;
import com.ai.repo.entity.RepoRating;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.RepoRatingMapper;
import com.ai.repo.service.SkillRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class RepoRatingServiceImplTest {

    @Mock
    private RepoRatingMapper repoRatingMapper;

    @Mock
    private SkillRepositoryService skillRepositoryService;

    private RepoRatingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RepoRatingServiceImpl();
        ReflectionTestUtils.setField(service, "repoRatingMapper", repoRatingMapper);
        ReflectionTestUtils.setField(service, "skillRepositoryService", skillRepositoryService);
    }

    private SkillRepository createSampleRepo(Long id, Long agentId, boolean isPublic) {
        SkillRepository r = new SkillRepository();
        r.setId(id);
        r.setAgentId(agentId);
        r.setUserId(1L);
        r.setSkillName("weather");
        r.setIsPublic(isPublic);
        return r;
    }

    private RepoRatingRequest createRequest(Long repoId, Integer rating) {
        RepoRatingRequest req = new RepoRatingRequest();
        req.setRepoId(repoId);
        req.setRating(rating);
        return req;
    }

    // ==================== rate() ====================

    @Test
    void rate_shouldSucceed_whenValidPublicRepo() {
        SkillRepository repo = createSampleRepo(1L, 10L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);

        RepoRatingRequest request = createRequest(1L, 5);
        RepoRatingResponse result = service.rate(request, 20L);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals(1L, result.getRepoId());
        verify(repoRatingMapper).upsert(any(RepoRating.class));
    }

    @Test
    void rate_shouldThrow_whenRepoIsPrivate() {
        SkillRepository repo = createSampleRepo(1L, 10L, false);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);

        assertThrows(BusinessException.class, () -> service.rate(createRequest(1L, 5), 20L));
        verify(repoRatingMapper, never()).upsert(any());
    }

    @Test
    void rate_shouldThrow_whenRatingOwnRepo() {
        SkillRepository repo = createSampleRepo(1L, 20L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);

        assertThrows(BusinessException.class, () -> service.rate(createRequest(1L, 5), 20L));
        verify(repoRatingMapper, never()).upsert(any());
    }

    @Test
    void rate_shouldUpsert_whenAlreadyRated() {
        SkillRepository repo = createSampleRepo(1L, 10L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);

        RepoRatingRequest request = createRequest(1L, 3);
        RepoRatingResponse result = service.rate(request, 20L);

        assertNotNull(result);
        assertEquals(3, result.getRating());
        verify(repoRatingMapper).upsert(any(RepoRating.class));
    }

    @Test
    void getRatingsByRepoId_shouldReturnEmpty_whenNoRatings() {
        SkillRepository repo = createSampleRepo(1L, 10L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);
        when(repoRatingMapper.selectByRepoIdWithAgent(1L)).thenReturn(List.of());

        List<RepoRatingResponse> result = service.getRatingsByRepoId(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getRatingsByAgentId_shouldReturnEmpty_whenNoRatings() {
        when(repoRatingMapper.selectByRaterAgentIdWithAgent(99L)).thenReturn(List.of());

        List<RepoRatingResponse> result = service.getRatingsByAgentId(99L);

        assertTrue(result.isEmpty());
    }

    // ==================== getAverageByRepoId() ====================

    @Test
    void getAverageByRepoId_shouldNotNpe_whenAvgIsNull() {
        SkillRepository repo = createSampleRepo(1L, 10L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);
        Map<String, Object> avgMap = new java.util.HashMap<>();
        avgMap.put("avg_rating", null);
        avgMap.put("total", null);
        when(repoRatingMapper.selectAvgByRepoId(1L)).thenReturn(avgMap);
        when(repoRatingMapper.selectDistributionByRepoId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> {
            RepoRatingAverageResponse result = service.getAverageByRepoId(1L);
            assertEquals(0.0, result.getAverageRating(), 0.001);
            assertEquals(0, result.getTotalRatings());
        });
    }

    @Test
    void getAverageByRepoId_shouldReturnEmpty_whenNoRatings() {
        SkillRepository repo = createSampleRepo(1L, 10L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);
        when(repoRatingMapper.selectAvgByRepoId(1L)).thenReturn(null);
        when(repoRatingMapper.selectDistributionByRepoId(1L)).thenReturn(List.of());

        RepoRatingAverageResponse result = service.getAverageByRepoId(1L);

        assertEquals(1L, result.getRepoId());
        assertEquals(0.0, result.getAverageRating(), 0.001);
        assertEquals(0, result.getTotalRatings());
        assertEquals(5, result.getDistribution().size());
        assertEquals(0, result.getDistribution().get(1));
        assertEquals(0, result.getDistribution().get(5));
    }

    @Test
    void getAverageByRepoId_shouldReturnCorrectAverage() {
        SkillRepository repo = createSampleRepo(1L, 10L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);
        when(repoRatingMapper.selectAvgByRepoId(1L)).thenReturn(Map.of("avg_rating", 4.0, "total", 2));
        when(repoRatingMapper.selectDistributionByRepoId(1L))
                .thenReturn(List.of(Map.of("rating", 4, "count", 1), Map.of("rating", 5, "count", 1)));

        RepoRatingAverageResponse result = service.getAverageByRepoId(1L);

        assertEquals(4.0, result.getAverageRating(), 0.001);
        assertEquals(2, result.getTotalRatings());
        assertEquals(1, result.getDistribution().get(4));
        assertEquals(1, result.getDistribution().get(5));
    }

    // ==================== getRatingsByRepoId() ====================

    @Test
    void getRatingsByRepoId_shouldReturnList() {
        SkillRepository repo = createSampleRepo(1L, 10L, true);
        when(skillRepositoryService.findById(1L)).thenReturn(repo);
        when(repoRatingMapper.selectByRepoIdWithAgent(1L))
                .thenReturn(List.of(Map.of(
                        "id", 1L, "repo_id", 1L, "rater_agent_id", 20L,
                        "rating", 5, "rater_agent_name", "AgentX",
                        "created_at", "2026-01-01T00:00:00"
                )));

        List<RepoRatingResponse> result = service.getRatingsByRepoId(1L);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getRating());
        assertEquals("AgentX", result.get(0).getRaterAgentName());
    }

    // ==================== getRatingsByAgentId() ====================

    @Test
    void getRatingsByAgentId_shouldReturnList() {
        when(repoRatingMapper.selectByRaterAgentIdWithAgent(20L))
                .thenReturn(List.of(Map.of(
                        "id", 1L, "repo_id", 1L, "rater_agent_id", 20L,
                        "rating", 4, "created_at", "2026-01-01T00:00:00"
                )));

        List<RepoRatingResponse> result = service.getRatingsByAgentId(20L);

        assertEquals(1, result.size());
        assertEquals(4, result.get(0).getRating());
        assertEquals(20L, result.get(0).getRaterAgentId());
    }
}
