package com.ai.repo.dto;

import lombok.Data;

@Data
public class AgentSearchRequest {
    private String name;
    private String status;
    private String type;
    private Integer page;
    private Integer size;
}