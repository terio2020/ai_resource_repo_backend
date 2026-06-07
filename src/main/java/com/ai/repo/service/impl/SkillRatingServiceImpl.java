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
import com.ai.repo.service.SkillRatingService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SkillRatingServiceImpl implements SkillRatingService {

    @Resource
    private SkillRatingMapper skillRatingMapper;

    @Resource
    private SkillMapper skillMapper;

    @Resource
    private AgentMapper agentMapper;

    @Override
    @Transactional
    public SkillRatingResponse rate(SkillRatingRequest request, Long raterAgentId) {
        Long skillId = request.getSkillId();
        Integer rating = request.getRating();

        // Validate skill exists
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new BusinessException(404, "Skill not found");
        }

        // Validate skill is public
        if (skill.getIsPublic() == null || !skill.getIsPublic()) {
            throw new BusinessException(400, "Cannot rate a non-public skill");
        }

        // Validate agent is not rating their own skill
        if (skill.getAgentId() != null && skill.getAgentId().equals(raterAgentId)) {
            throw new BusinessException(400, "Cannot rate your own skill");
        }

        // Validate rater agent exists
        Agent raterAgent = agentMapper.selectById(raterAgentId);
        if (raterAgent == null) {
            throw new BusinessException(404, "Rating agent not found");
        }

        // Upsert the rating
        SkillRating skillRating = new SkillRating();
        skillRating.setSkillId(skillId);
        skillRating.setRaterAgentId(raterAgentId);
        skillRating.setRating(rating);
        skillRatingMapper.upsert(skillRating);

        // Fetch the saved record to get the generated ID and timestamps
        SkillRating saved = skillRatingMapper.selectBySkillAndRater(skillId, raterAgentId);

        return buildResponse(saved, raterAgent.getName());
    }

    @Override
    public SkillRatingAverageResponse getAverageBySkillId(Long skillId) {
        // Validate skill exists
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new BusinessException(404, "Skill not found");
        }

        Map<String, Object> avgResult = skillRatingMapper.selectAvgBySkillId(skillId);
        List<Map<String, Object>> distributionResult = skillRatingMapper.selectDistributionBySkillId(skillId);

        Double avgRating = 0.0;
        Integer totalRatings = 0;

        if (avgResult != null && avgResult.get("avg_rating") != null) {
            BigDecimal avg = (BigDecimal) avgResult.get("avg_rating");
            avgRating = avg.setScale(2, RoundingMode.HALF_UP).doubleValue();
            totalRatings = ((Number) avgResult.get("total_ratings")).intValue();
        }

        // Build distribution map (rating 1-5 -> count)
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }
        if (distributionResult != null) {
            for (Map<String, Object> row : distributionResult) {
                Integer rating = ((Number) row.get("rating")).intValue();
                Integer count = ((Number) row.get("count")).intValue();
                distribution.put(rating, count);
            }
        }

        return SkillRatingAverageResponse.builder()
                .skillId(skillId)
                .averageRating(avgRating)
                .totalRatings(totalRatings)
                .distribution(distribution)
                .build();
    }

    @Override
    public List<SkillRatingResponse> getRatingsBySkillId(Long skillId) {
        List<Map<String, Object>> rows = skillRatingMapper.selectBySkillIdWithAgent(skillId);
        return rows.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillRatingResponse> getRatingsByAgentId(Long raterAgentId) {
        List<Map<String, Object>> rows = skillRatingMapper.selectByRaterAgentIdWithAgent(raterAgentId);
        return rows.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SkillRatingResponse buildResponse(SkillRating rating, String agentName) {
        return SkillRatingResponse.builder()
                .id(rating.getId())
                .skillId(rating.getSkillId())
                .raterAgentId(rating.getRaterAgentId())
                .raterAgentName(agentName)
                .rating(rating.getRating())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private SkillRatingResponse mapToResponse(Map<String, Object> row) {
        return SkillRatingResponse.builder()
                .id(((Number) row.get("id")).longValue())
                .skillId(((Number) row.get("skillId")).longValue())
                .raterAgentId(((Number) row.get("raterAgentId")).longValue())
                .raterAgentName((String) row.get("raterAgentName"))
                .rating(((Number) row.get("rating")).intValue())
                .createdAt(parseTimestamp(row.get("createdAt")))
                .updatedAt(parseTimestamp(row.get("updatedAt")))
                .build();
    }

    private java.time.LocalDateTime parseTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) value;
        }
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        if (value instanceof String) {
            return java.time.LocalDateTime.parse((String) value);
        }
        return null;
    }
}
