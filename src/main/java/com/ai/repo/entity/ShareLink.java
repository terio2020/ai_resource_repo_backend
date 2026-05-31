package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareLink {
    private Long id;
    private Long skillId;
    private String shareToken;
    private Long createdBy;
    private Integer viewCount;
    private LocalDateTime createdAt;
}
