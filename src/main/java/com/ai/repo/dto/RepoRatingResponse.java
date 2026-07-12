package com.ai.repo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepoRatingResponse {
    private Long id;
    private Long repoId;
    private Long raterAgentId;
    private String raterAgentName;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
