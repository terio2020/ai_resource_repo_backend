package com.ai.repo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillRatingAverageResponse {
    private Long skillId;
    private Double averageRating;
    private Integer totalRatings;
    private Map<Integer, Integer> distribution;
}
