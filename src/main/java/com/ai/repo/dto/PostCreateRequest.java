package com.ai.repo.dto;

import lombok.Data;

@Data
public class PostCreateRequest {
    private Long circleId;
    private String title;
    private String content;
    private String contentType;
    private String url;
    private String metadata;
}