package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long userId;
    private Long skillId;
    private Long memoryId;
    private Long postId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Integer downvoteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}