package com.ai.repo.mapper;

import com.ai.repo.entity.AgentSkillAssociation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentSkillAssociationMapper {
    AgentSkillAssociation findById(String id);
    List<AgentSkillAssociation> findByAgentId(String agentId);
    List<AgentSkillAssociation> findBySkillId(String skillId);
    AgentSkillAssociation findByAgentAndSkill(String agentId, String skillId);
    List<AgentSkillAssociation> findAll();
    int insert(AgentSkillAssociation association);
    int update(AgentSkillAssociation association);
    int deleteById(String id);
    int deleteByAgentId(String agentId);
    int deleteBySkillId(String skillId);
}
