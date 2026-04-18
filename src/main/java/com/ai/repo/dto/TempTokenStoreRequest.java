package com.ai.repo.dto;

import lombok.Data;

@Data
public class TempTokenStoreRequest {
    private String accessToken;
    private String sessionId;
}
