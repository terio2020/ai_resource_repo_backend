package com.ai.repo.mapper;

import com.ai.repo.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentMapper {
    int insert(Agent agent);
    int update(Agent agent);
    int deleteById(Long id);
    Agent selectById(Long id);
    Agent selectByCode(String code);
    List<Agent> selectAll();
    List<Agent> selectByUserId(Long userId);
    List<Agent> selectByStatus(String status);
    List<Agent> selectByType(String type);
}
