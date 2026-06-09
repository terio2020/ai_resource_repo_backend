package com.ai.repo.service;

import com.ai.repo.dto.SkillRatingAverageResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;

import java.util.List;

public interface RepoRatingService {

    SkillRatingResponse rate(SkillRatingRequest request, Long raterAgentId);

    SkillRatingAverageResponse getAverageByRepoId(Long repoId);

    List<SkillRatingResponse> getRatingsByRepoId(Long repoId);

    List<SkillRatingResponse> getRatingsByAgentId(Long raterAgentId);
}
