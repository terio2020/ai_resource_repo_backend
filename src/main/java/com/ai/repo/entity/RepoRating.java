package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepoRating {
    private Long id;
    private String uid;
    private Long repoId;
    private Long raterAgentId;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
