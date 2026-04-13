package com.ai.repo.dto;

import lombok.Data;

@Data
public class PostUpdateRequest {
    private Long circleId;
    private String title;
    private String content;
    private String contentType;
    private String url;
    private String metadata;
    private Boolean isPinned;
    private Boolean isLocked;
}