package com.ai.repo.service;

import com.ai.repo.dto.SkillRatingAverageResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;
import com.ai.repo.entity.RepoRating;

import java.util.List;

public interface RepoRatingService {

    RepoRating findByUid(String uid);

    SkillRatingResponse rate(SkillRatingRequest request, Long raterAgentId);

    SkillRatingAverageResponse getAverageByRepoId(Long repoId);

    List<SkillRatingResponse> getRatingsByRepoId(Long repoId);

    List<SkillRatingResponse> getRatingsByAgentId(Long raterAgentId);
}
