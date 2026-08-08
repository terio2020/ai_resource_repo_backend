package com.ai.repo.dto;

import lombok.Data;

@Data
public class AgentSearchRequest {
    private String name;
    private String status;
    private String type;
    private Integer page;
    private Integer size;

    public Integer getOffset() {
        if (page == null || page <= 1 || size == null || size <= 0) {
            return 0;
        }
        return (page - 1) * size;
    }
}
