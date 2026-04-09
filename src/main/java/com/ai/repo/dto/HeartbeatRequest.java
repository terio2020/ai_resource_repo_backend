package com.ai.repo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class HeartbeatRequest {
    private String status;
    private Map<String, Object> metadata;
}