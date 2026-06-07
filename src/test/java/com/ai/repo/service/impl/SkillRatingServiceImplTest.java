package com.ai.repo.service.impl;

import com.ai.repo.dto.SkillRatingAverageResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.Skill;
import com.ai.repo.entity.SkillRating;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.SkillMapper;
import com.ai.repo.mapper.SkillRatingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillRatingServiceImplTest {

    @Mock
    private SkillRatingMapper skillRatingMapper;

    @Mock
    private SkillMapper skillMapper;

    @Mock
    private AgentMapper agentMapper;

    private SkillRatingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SkillRatingServiceImpl();
        try {
            java.lang.reflect.Field ratingMapperField = SkillRatingServiceImpl.class.getDeclaredField("skillRatingMapper");
            ratingMapperField.setAccessible(true);
            ratingMapperField.set(service, skillRatingMapper);

            java.lang.reflect.Field skillField = SkillRatingServiceImpl.class.getDeclaredField("skillMapper");
            skillField.setAccessible(true);
            skillField.set(service, skillMapper);

            java.lang.reflect.Field agentField = SkillRatingServiceImpl.class.getDeclaredField("agentMapper");
            agentField.setAccessible(true);
            agentField.set(service, agentMapper);
        } catch (Exception e) {
            fail("Failed to inject mock mappers: " + e.getMessage());
        }
    }

    // ========== Helper Methods ==========

    private Skill createSampleSkill(Long id, Long agentId, boolean isPublic) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setUserId(1L);
        skill.setAgentId(agentId);
        skill.setName("test-skill");
        skill.setIsPublic(isPublic);
        return skill;
    }

    private Agent createSampleAgent(Long id, String name) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setName(name);
        agent.setUserId(1L);
        return agent;
    }

    private SkillRating createSampleRating(Long id, Long skillId, Long raterAgentId, Integer rating) {
        SkillRating sr = new SkillRating();
        sr.setId(id);
        sr.setSkillId(skillId);
        sr.setRaterAgentId(raterAgentId);
        sr.setRating(rating);
        sr.setCreatedAt(LocalDateTime.now());
        sr.setUpdatedAt(LocalDateTime.now());
        return sr;
    }

    private SkillRatingRequest createRatingRequest(Long skillId, Integer rating) {
        SkillRatingRequest req = new SkillRatingRequest();
        req.setSkillId(skillId);
        req.setRating(rating);
        return req;
    }

    // ========== rate() Tests ==========

    @Test
    void rate_shouldSucceed_whenValidPublicSkill() {
        // Given
        Long skillId = 1L;
        Long skillOwnerAgentId = 10L;
        Long raterAgentId = 20L;

        Skill skill = createSampleSkill(skillId, skillOwnerAgentId, true);
        Agent raterAgent = createSampleAgent(raterAgentId, "Rater Agent");
        SkillRatingRequest request = createRatingRequest(skillId, 5);

        SkillRating savedRating = createSampleRating(1L, skillId, raterAgentId, 5);

        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(agentMapper.selectById(raterAgentId)).thenReturn(raterAgent);
        when(skillRatingMapper.upsert(any(SkillRating.class))).thenReturn(1);
        when(skillRatingMapper.selectBySkillAndRater(skillId, raterAgentId)).thenReturn(savedRating);

        // When
        SkillRatingResponse response = service.rate(request, raterAgentId);

        // Then
        assertNotNull(response);
        assertEquals(skillId, response.getSkillId());
        assertEquals(raterAgentId, response.getRaterAgentId());
        assertEquals(5, response.getRating());
        assertEquals("Rater Agent", response.getRaterAgentName());
        verify(skillRatingMapper).upsert(argThat(r ->
                r.getSkillId().equals(skillId) &&
                r.getRaterAgentId().equals(raterAgentId) &&
                r.getRating() == 5
        ));
        verify(skillRatingMapper).selectBySkillAndRater(skillId, raterAgentId);
    }

    @Test
    void rate_shouldThrow_whenSkillNotFound() {
        // Given
        Long skillId = 999L;
        Long raterAgentId = 20L;

        when(skillMapper.selectById(skillId)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.rate(createRatingRequest(skillId, 4), raterAgentId));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("not found"));
        verify(skillRatingMapper, never()).upsert(any());
    }

    @Test
    void rate_shouldThrow_whenSkillIsNotPublic() {
        // Given
        Long skillId = 1L;
        Long raterAgentId = 20L;

        Skill skill = createSampleSkill(skillId, 10L, false);
        when(skillMapper.selectById(skillId)).thenReturn(skill);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.rate(createRatingRequest(skillId, 4), raterAgentId));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("non-public"));
        verify(skillRatingMapper, never()).upsert(any());
    }

    @Test
    void rate_shouldThrow_whenSkillIsPublicNull() {
        // Given
        Long skillId = 1L;
        Long raterAgentId = 20L;

        Skill skill = createSampleSkill(skillId, 10L, true);
        skill.setIsPublic(null);
        when(skillMapper.selectById(skillId)).thenReturn(skill);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.rate(createRatingRequest(skillId, 4), raterAgentId));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("non-public"));
        verify(skillRatingMapper, never()).upsert(any());
    }

    @Test
    void rate_shouldThrow_whenRatingOwnSkill() {
        // Given
        Long skillId = 1L;
        Long agentId = 10L;

        Skill skill = createSampleSkill(skillId, agentId, true);
        when(skillMapper.selectById(skillId)).thenReturn(skill);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.rate(createRatingRequest(skillId, 4), agentId));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("own skill"));
        verify(skillRatingMapper, never()).upsert(any());
    }

    @Test
    void rate_shouldSucceed_whenSkillHasNoOwnerAgent() {
        // Given: skill with null agentId (not owned by any agent)
        Long skillId = 1L;
        Long raterAgentId = 20L;

        Skill skill = createSampleSkill(skillId, null, true);
        Agent raterAgent = createSampleAgent(raterAgentId, "Rater");
        SkillRating savedRating = createSampleRating(1L, skillId, raterAgentId, 3);

        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(agentMapper.selectById(raterAgentId)).thenReturn(raterAgent);
        when(skillRatingMapper.upsert(any(SkillRating.class))).thenReturn(1);
        when(skillRatingMapper.selectBySkillAndRater(skillId, raterAgentId)).thenReturn(savedRating);

        // When
        SkillRatingResponse response = service.rate(createRatingRequest(skillId, 3), raterAgentId);

        // Then
        assertNotNull(response);
        assertEquals(3, response.getRating());
    }

    @Test
    void rate_shouldThrow_whenRaterAgentNotFound() {
        // Given
        Long skillId = 1L;
        Long raterAgentId = 999L;

        Skill skill = createSampleSkill(skillId, 10L, true);
        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(agentMapper.selectById(raterAgentId)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.rate(createRatingRequest(skillId, 4), raterAgentId));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("not found"));
        verify(skillRatingMapper, never()).upsert(any());
    }

    @Test
    void rate_shouldUpsert_whenAlreadyRated() {
        // Given: agent already rated this skill, should update instead of insert
        Long skillId = 1L;
        Long raterAgentId = 20L;

        Skill skill = createSampleSkill(skillId, 10L, true);
        Agent raterAgent = createSampleAgent(raterAgentId, "Rater");
        SkillRating existingRating = createSampleRating(5L, skillId, raterAgentId, 4);

        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(agentMapper.selectById(raterAgentId)).thenReturn(raterAgent);
        when(skillRatingMapper.upsert(any(SkillRating.class))).thenReturn(1);
        when(skillRatingMapper.selectBySkillAndRater(skillId, raterAgentId)).thenReturn(existingRating);

        // When
        SkillRatingResponse response = service.rate(createRatingRequest(skillId, 4), raterAgentId);

        // Then
        assertEquals(4, response.getRating());
        verify(skillRatingMapper).upsert(argThat(r ->
                r.getSkillId().equals(skillId) &&
                r.getRaterAgentId().equals(raterAgentId) &&
                r.getRating() == 4
        ));
    }

    // ========== getAverageBySkillId() Tests ==========

    @Test
    void getAverageBySkillId_shouldReturnCorrectAverage() {
        // Given
        Long skillId = 1L;
        Skill skill = createSampleSkill(skillId, 10L, true);

        Map<String, Object> avgMap = new HashMap<>();
        avgMap.put("avg_rating", new BigDecimal("4.25"));
        avgMap.put("total_ratings", 8L);

        List<Map<String, Object>> distribution = Arrays.asList(
                createDistRow(5, 3L),
                createDistRow(4, 4L),
                createDistRow(3, 1L)
        );

        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(skillRatingMapper.selectAvgBySkillId(skillId)).thenReturn(avgMap);
        when(skillRatingMapper.selectDistributionBySkillId(skillId)).thenReturn(distribution);

        // When
        SkillRatingAverageResponse response = service.getAverageBySkillId(skillId);

        // Then
        assertNotNull(response);
        assertEquals(skillId, response.getSkillId());
        assertEquals(4.25, response.getAverageRating());
        assertEquals(8, response.getTotalRatings());
        assertNotNull(response.getDistribution());
        assertEquals(0, response.getDistribution().get(1));
        assertEquals(0, response.getDistribution().get(2));
        assertEquals(1, response.getDistribution().get(3));
        assertEquals(4, response.getDistribution().get(4));
        assertEquals(3, response.getDistribution().get(5));
    }

    @Test
    void getAverageBySkillId_shouldReturnZero_whenNoRatings() {
        // Given
        Long skillId = 1L;
        Skill skill = createSampleSkill(skillId, 10L, true);

        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(skillRatingMapper.selectAvgBySkillId(skillId)).thenReturn(null);
        when(skillRatingMapper.selectDistributionBySkillId(skillId)).thenReturn(null);

        // When
        SkillRatingAverageResponse response = service.getAverageBySkillId(skillId);

        // Then
        assertNotNull(response);
        assertEquals(skillId, response.getSkillId());
        assertEquals(0.0, response.getAverageRating());
        assertEquals(0, response.getTotalRatings());
        // Distribution should have all zeros
        assertEquals(0, response.getDistribution().get(1));
        assertEquals(0, response.getDistribution().get(2));
        assertEquals(0, response.getDistribution().get(3));
        assertEquals(0, response.getDistribution().get(4));
        assertEquals(0, response.getDistribution().get(5));
    }

    @Test
    void getAverageBySkillId_shouldReturnZero_whenAvgIsNull() {
        // Given
        Long skillId = 1L;
        Skill skill = createSampleSkill(skillId, 10L, true);

        Map<String, Object> avgMap = new HashMap<>();
        avgMap.put("avg_rating", null);
        avgMap.put("total_ratings", 0L);

        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(skillRatingMapper.selectAvgBySkillId(skillId)).thenReturn(avgMap);
        when(skillRatingMapper.selectDistributionBySkillId(skillId)).thenReturn(Collections.emptyList());

        // When
        SkillRatingAverageResponse response = service.getAverageBySkillId(skillId);

        // Then
        assertEquals(0.0, response.getAverageRating());
        assertEquals(0, response.getTotalRatings());
    }

    @Test
    void getAverageBySkillId_shouldThrow_whenSkillNotFound() {
        // Given
        Long skillId = 999L;
        when(skillMapper.selectById(skillId)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAverageBySkillId(skillId));
        assertEquals(404, ex.getCode());
        verify(skillRatingMapper, never()).selectAvgBySkillId(any());
    }

    // ========== getRatingsBySkillId() Tests ==========

    @Test
    void getRatingsBySkillId_shouldReturnRatingsWithAgentNames() {
        // Given
        Long skillId = 1L;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1L);
        row1.put("skillId", skillId);
        row1.put("raterAgentId", 10L);
        row1.put("rating", 5);
        row1.put("raterAgentName", "Agent A");
        row1.put("createdAt", now);
        row1.put("updatedAt", now);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2L);
        row2.put("skillId", skillId);
        row2.put("raterAgentId", 11L);
        row2.put("rating", 3);
        row2.put("raterAgentName", "Agent B");
        row2.put("createdAt", now);
        row2.put("updatedAt", now);

        List<Map<String, Object>> rows = Arrays.asList(row1, row2);
        when(skillRatingMapper.selectBySkillIdWithAgent(skillId)).thenReturn(rows);

        // When
        List<SkillRatingResponse> responses = service.getRatingsBySkillId(skillId);

        // Then
        assertEquals(2, responses.size());
        assertEquals("Agent A", responses.get(0).getRaterAgentName());
        assertEquals(5, responses.get(0).getRating());
        assertEquals("Agent B", responses.get(1).getRaterAgentName());
        assertEquals(3, responses.get(1).getRating());
    }

    @Test
    void getRatingsBySkillId_shouldReturnEmptyList_whenNoRatings() {
        // Given
        Long skillId = 1L;
        when(skillRatingMapper.selectBySkillIdWithAgent(skillId)).thenReturn(Collections.emptyList());

        // When
        List<SkillRatingResponse> responses = service.getRatingsBySkillId(skillId);

        // Then
        assertTrue(responses.isEmpty());
    }

    // ========== getRatingsByAgentId() Tests ==========

    @Test
    void getRatingsByAgentId_shouldReturnRatingsByAgent() {
        // Given
        Long raterAgentId = 10L;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        Map<String, Object> row = new HashMap<>();
        row.put("id", 1L);
        row.put("skillId", 1L);
        row.put("raterAgentId", raterAgentId);
        row.put("rating", 4);
        row.put("raterAgentName", "Agent A");
        row.put("createdAt", now);
        row.put("updatedAt", now);

        when(skillRatingMapper.selectByRaterAgentIdWithAgent(raterAgentId))
                .thenReturn(Collections.singletonList(row));

        // When
        List<SkillRatingResponse> responses = service.getRatingsByAgentId(raterAgentId);

        // Then
        assertEquals(1, responses.size());
        assertEquals(raterAgentId, responses.get(0).getRaterAgentId());
        assertEquals(4, responses.get(0).getRating());
    }

    @Test
    void getRatingsByAgentId_shouldReturnEmptyList_whenNoRatings() {
        // Given
        Long raterAgentId = 10L;
        when(skillRatingMapper.selectByRaterAgentIdWithAgent(raterAgentId))
                .thenReturn(Collections.emptyList());

        // When
        List<SkillRatingResponse> responses = service.getRatingsByAgentId(raterAgentId);

        // Then
        assertTrue(responses.isEmpty());
    }

    // ========== Helper ==========

    private Map<String, Object> createDistRow(Integer rating, Long count) {
        Map<String, Object> row = new HashMap<>();
        row.put("rating", rating);
        row.put("count", count);
        return row;
    }
}
