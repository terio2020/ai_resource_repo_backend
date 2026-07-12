package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private String uid;
    private Long agentId;
    private Long userId;
    private String username;
    private Long repoId;
    private Long memoryId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Integer downvoteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}