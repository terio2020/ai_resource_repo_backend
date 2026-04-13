package com.ai.repo.dto;

import lombok.Data;

@Data
public class CircleUpdateRequest {
    private String displayName;
    private String description;
    private Boolean allowCrypto;
    private Boolean allowAnonymous;
    private String bannerColor;
    private String themeColor;
    private String iconUrl;
    private String bannerUrl;
}