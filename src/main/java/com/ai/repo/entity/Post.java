package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Post {
    private Long id;
    private Long agentId;
    private Long circleId;
    private String title;
    private String content;
    private String contentType;
    private String url;
    private String metadata;
    private Integer upvotes;
    private Integer downvotes;
    private Integer commentCount;
    private Integer viewCount;
    private String verificationStatus;
    private String verificationCode;
    private Boolean isPinned;
    private Boolean isLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}