package com.ai.repo.service.impl;

import com.ai.repo.dto.RepoRatingAverageResponse;
import com.ai.repo.dto.RepoRatingRequest;
import com.ai.repo.dto.RepoRatingResponse;
import com.ai.repo.entity.RepoRating;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.RepoRatingMapper;
import com.ai.repo.service.RepoRatingService;
import com.ai.repo.service.SkillRepositoryService;
import com.ai.repo.util.UuidUtil;
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
    public RepoRating findByUid(String uid) {
        return repoRatingMapper.selectByUid(uid);
    }

    @Override
    public RepoRatingResponse rate(RepoRatingRequest request, Long raterAgentId) {
        var repo = skillRepositoryService.findById(request.getRepoId());

        if (!Boolean.TRUE.equals(repo.getIsPublic())) {
            throw new BusinessException(400, "Can only rate public repositories");
        }

        if (repo.getAgentId() != null && repo.getAgentId().equals(raterAgentId)) {
            throw new BusinessException(400, "Cannot rate your own repository");
        }

        RepoRating rating = new RepoRating();
        if (rating.getUid() == null || rating.getUid().isEmpty()) {
            rating.setUid(UuidUtil.generate());
        }
        rating.setRepoId(request.getRepoId());
        rating.setRaterAgentId(raterAgentId);
        rating.setRating(request.getRating());
        repoRatingMapper.upsert(rating);

        log.info("Agent {} rated repository {} with {}", raterAgentId, request.getRepoId(), request.getRating());
        return toResponse(rating, null);
    }

    @Override
    public RepoRatingAverageResponse getAverageByRepoId(Long repoId) {
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

        RepoRatingAverageResponse response = new RepoRatingAverageResponse();
        response.setRepoId(repoId);
        response.setAverageRating(avg != null && avg.get("avg_rating") != null ? ((Number) avg.get("avg_rating")).doubleValue() : 0.0);
        response.setTotalRatings(avg != null && avg.get("total") != null ? ((Number) avg.get("total")).intValue() : 0);
        response.setDistribution(distribution);
        return response;
    }

    @Override
    public List<RepoRatingResponse> getRatingsByRepoId(Long repoId) {
        skillRepositoryService.findById(repoId);
        List<Map<String, Object>> rows = repoRatingMapper.selectByRepoIdWithAgent(repoId);
        List<RepoRatingResponse> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(toResponseFromMap(row));
        }
        return result;
    }

    @Override
    public List<RepoRatingResponse> getRatingsByAgentId(Long raterAgentId) {
        List<Map<String, Object>> rows = repoRatingMapper.selectByRaterAgentIdWithAgent(raterAgentId);
        List<RepoRatingResponse> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            RepoRatingResponse resp = toResponseFromMap(row);
            result.add(resp);
        }
        return result;
    }

    private RepoRatingResponse toResponse(RepoRating rating, String raterName) {
        RepoRatingResponse resp = new RepoRatingResponse();
        resp.setId(rating.getId());
        resp.setRepoId(rating.getRepoId());
        resp.setRaterAgentId(rating.getRaterAgentId());
        resp.setRating(rating.getRating());
        resp.setRaterAgentName(raterName);
        resp.setCreatedAt(rating.getCreatedAt());
        return resp;
    }

    @SuppressWarnings("unchecked")
    private RepoRatingResponse toResponseFromMap(Map<String, Object> map) {
        RepoRatingResponse resp = new RepoRatingResponse();
        resp.setId(((Number) map.get("id")).longValue());
        resp.setRepoId(((Number) map.get("repo_id")).longValue());
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
