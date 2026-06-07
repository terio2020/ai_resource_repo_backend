package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SkillRating {
    private Long id;
    private Long skillId;
    private Long raterAgentId;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
