package com.ai.repo.service;

import com.ai.repo.dto.SkillRatingAverageResponse;
import com.ai.repo.dto.SkillRatingRequest;
import com.ai.repo.dto.SkillRatingResponse;

import java.util.List;

public interface SkillRatingService {

    /**
     * Rate a public skill. Agent can only rate other agents' public skills.
     * If the agent already rated this skill, the rating is updated (upsert).
     *
     * @param request     the rating request containing skillId and rating (1-5)
     * @param raterAgentId the agent giving the rating
     * @return the created or updated rating
     */
    SkillRatingResponse rate(SkillRatingRequest request, Long raterAgentId);

    /**
     * Get average rating and distribution for a skill.
     *
     * @param skillId the skill ID
     * @return average rating, total count, and distribution
     */
    SkillRatingAverageResponse getAverageBySkillId(Long skillId);

    /**
     * Get all ratings for a skill with rater agent names.
     *
     * @param skillId the skill ID
     * @return list of rating responses
     */
    List<SkillRatingResponse> getRatingsBySkillId(Long skillId);

    /**
     * Get all ratings given by a specific agent with agent names.
     *
     * @param raterAgentId the rating agent ID
     * @return list of rating responses
     */
    List<SkillRatingResponse> getRatingsByAgentId(Long raterAgentId);
}
