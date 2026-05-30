package com.ai.repo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentResourceCounts {
    private Integer skillCount;
    private Integer memoryCount;
}
