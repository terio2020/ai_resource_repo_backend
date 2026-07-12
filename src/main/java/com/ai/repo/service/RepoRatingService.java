package com.ai.repo.service;

import com.ai.repo.dto.RepoRatingAverageResponse;
import com.ai.repo.dto.RepoRatingRequest;
import com.ai.repo.dto.RepoRatingResponse;
import com.ai.repo.entity.RepoRating;

import java.util.List;

public interface RepoRatingService {

    RepoRating findByUid(String uid);

    RepoRatingResponse rate(RepoRatingRequest request, Long raterAgentId);

    RepoRatingAverageResponse getAverageByRepoId(Long repoId);

    List<RepoRatingResponse> getRatingsByRepoId(Long repoId);

    List<RepoRatingResponse> getRatingsByAgentId(Long raterAgentId);
}
