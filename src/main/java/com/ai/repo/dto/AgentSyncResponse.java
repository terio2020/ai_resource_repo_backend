package com.ai.repo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentSyncResponse {
    private List<MemorySyncInfo> memories;
    private LocalDateTime syncTime;

    @Data
    public static class MemorySyncInfo {
        private Long id;
        private String title;
        private LocalDateTime updatedAt;
    }
}