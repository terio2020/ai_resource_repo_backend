package com.ai.repo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentSkillAssociation {
    private String id;
    private String agentId;
    private String skillId;
    private Double proficiency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
