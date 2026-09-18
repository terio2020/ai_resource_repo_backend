package com.ai.repo.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class SkillRepository {
    private Long id;
    private String uid;
    private Long agentId;
    private Long userId;
    private String skillName;
    private String version;
    private String description;
    private String tags;
    private String category;
    private String type;
    private Boolean enabled;
    private Boolean isPublic;
    private String status;
    private String shareId;
    @JsonIgnore
    private String repoPath;
    private Long parentId;
    private Integer downloadCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getGitPath() {
        if (agentId == null || skillName == null || skillName.isBlank()) {
            return null;
        }
        return "/api/git/agent_" + agentId + "/" + skillName + ".git";
    }
}
