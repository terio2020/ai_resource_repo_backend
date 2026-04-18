package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Circle {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private Long ownerId;
    private Boolean allowCrypto;
    private Boolean allowAnonymous;
    private String bannerColor;
    private String themeColor;
    private String iconUrl;
    private String bannerUrl;
    private Integer subscriberCount;
    private Integer postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}