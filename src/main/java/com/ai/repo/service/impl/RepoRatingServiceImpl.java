package com.ai.repo.service.impl;

import com.ai.repo.dto.SkillRatingAverageResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;
import com.ai.repo.entity.RepoRating;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.RepoRatingMapper;
import com.ai.repo.service.RepoRatingService;
import com.ai.repo.service.SkillRepositoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RepoRatingServiceImpl implements RepoRatingService {

    @Resource
    private RepoRatingMapper repoRatingMapper;

    @Resource
    private SkillRepositoryService skillRepositoryService;

    @Override
    public SkillRatingResponse rate(SkillRatingRequest request, Long raterAgentId) {
        var repo = skillRepositoryService.findById(request.getSkillId());

        if (!Boolean.TRUE.equals(repo.getIsPublic())) {
            throw new BusinessException(400, "Can only rate public repositories");
        }

        if (repo.getAgentId() != null && repo.getAgentId().equals(raterAgentId)) {
            throw new BusinessException(400, "Cannot rate your own repository");
        }

        RepoRating rating = new RepoRating();
        rating.setRepoId(request.getSkillId());
        rating.setRaterAgentId(raterAgentId);
        rating.setRating(request.getRating());
        repoRatingMapper.upsert(rating);

        log.info("Agent {} rated repository {} with {}", raterAgentId, request.getSkillId(), request.getRating());
        return toResponse(rating, null);
    }

    @Override
    public SkillRatingAverageResponse getAverageByRepoId(Long repoId) {
        skillRepositoryService.findById(repoId);

        Map<String, Object> avg = repoRatingMapper.selectAvgByRepoId(repoId);
        List<Map<String, Object>> dist = repoRatingMapper.selectDistributionByRepoId(repoId);

        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }
        for (Map<String, Object> row : dist) {
            distribution.put(((Number) row.get("rating")).intValue(),
                    ((Number) row.get("count")).intValue());
        }

        SkillRatingAverageResponse response = new SkillRatingAverageResponse();
        response.setSkillId(repoId);
        response.setAverageRating(avg != null ? ((Number) avg.get("avg_rating")).doubleValue() : 0.0);
        response.setTotalRatings(avg != null ? ((Number) avg.get("total")).intValue() : 0);
        response.setDistribution(distribution);
        return response;
    }

    @Override
    public List<SkillRatingResponse> getRatingsByRepoId(Long repoId) {
        skillRepositoryService.findById(repoId);
        List<Map<String, Object>> rows = repoRatingMapper.selectByRepoIdWithAgent(repoId);
        List<SkillRatingResponse> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(toResponseFromMap(row));
        }
        return result;
    }

    @Override
    public List<SkillRatingResponse> getRatingsByAgentId(Long raterAgentId) {
        List<Map<String, Object>> rows = repoRatingMapper.selectByRaterAgentIdWithAgent(raterAgentId);
        List<SkillRatingResponse> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            SkillRatingResponse resp = toResponseFromMap(row);
            result.add(resp);
        }
        return result;
    }

    private SkillRatingResponse toResponse(RepoRating rating, String raterName) {
        SkillRatingResponse resp = new SkillRatingResponse();
        resp.setId(rating.getId());
        resp.setSkillId(rating.getRepoId());
        resp.setRaterAgentId(rating.getRaterAgentId());
        resp.setRating(rating.getRating());
        resp.setRaterAgentName(raterName);
        resp.setCreatedAt(rating.getCreatedAt());
        return resp;
    }

    @SuppressWarnings("unchecked")
    private SkillRatingResponse toResponseFromMap(Map<String, Object> map) {
        SkillRatingResponse resp = new SkillRatingResponse();
        resp.setId(((Number) map.get("id")).longValue());
        resp.setSkillId(((Number) map.get("repo_id")).longValue());
        resp.setRaterAgentId(((Number) map.get("rater_agent_id")).longValue());
        resp.setRating(((Number) map.get("rating")).intValue());

        Object name = map.get("rater_agent_name");
        if (name != null) resp.setRaterAgentName(name.toString());

        Object createdAt = map.get("created_at");
        if (createdAt != null) {
            resp.setCreatedAt(java.time.LocalDateTime.parse(createdAt.toString().replace(" ", "T")));
        }
        return resp;
    }
}
