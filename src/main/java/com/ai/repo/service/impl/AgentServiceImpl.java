package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.dto.AgentStatsResponse;
import com.ai.repo.dto.AgentSyncResponse;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.Skill;
import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.SkillMapper;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private MemoryMapper memoryMapper;

    @Override
    public Agent create(Agent agent) {
        if (agentMapper.selectByCode(agent.getCode()) != null) {
            throw new BusinessException("Agent code already exists");
        }
        agentMapper.insert(agent);
        return agent;
    }

    @Override
    public Agent update(Agent agent) {
        if (agentMapper.selectById(agent.getId()) == null) {
            throw new BusinessException("Agent not found");
        }
        agentMapper.update(agent);
        return agent;
    }

    @Override
    public boolean delete(Long id) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.deleteById(id) > 0;
    }

    @Override
    public Agent findById(Long id) {
        return agentMapper.selectById(id);
    }

    @Override
    public Agent findByCode(String code) {
        return agentMapper.selectByCode(code);
    }

    @Override
    public List<Agent> findAll() {
        return agentMapper.selectAll();
    }

    @Override
    public List<Agent> findByUserId(Long userId) {
        return agentMapper.selectByUserId(userId);
    }

    @Override
    public List<Agent> findByStatus(String status) {
        return agentMapper.selectByStatus(status);
    }

    @Override
    public List<Agent> findByType(String type) {
        return agentMapper.selectByType(type);
    }

    @Override
    public PageResult<Agent> findPage(Integer page, Integer size) {
        int actualPage = page != null && page > 0 ? page : 1;
        int actualSize = size != null && size > 0 ? size : 10;
        int offset = (actualPage - 1) * actualSize;
        
        List<Agent> records = agentMapper.selectPage(actualPage, actualSize, offset);
        
        Long total = agentMapper.countTotal();
        Long pages = (total + actualSize - 1) / actualSize;
        
        return new PageResult<>(records, total, (long) actualPage, (long) actualSize);
    }

    @Override
    public List<Agent> findBySearch(AgentSearchRequest request) {
        if (request.getPage() == null || request.getPage() < 1) {
            request.setPage(1);
        }
        if (request.getSize() == null || request.getSize() < 1) {
            request.setSize(10);
        }
        int offset = (request.getPage() - 1) * request.getSize();
        
        return agentMapper.selectBySearch(request);
    }

    @Override
    public AgentStatsResponse getStats(Long agentId) {
        if (agentMapper.selectById(agentId) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.selectStats(agentId);
    }

    @Override
    public boolean updateHeartbeat(Long id, String status, String lastHeartbeatAt) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.updateHeartbeat(id, status, lastHeartbeatAt) > 0;
    }

    @Override
    public boolean updateStatusOnly(Long id, String status) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.updateStatusOnly(id, status) > 0;
    }

    @Override
    public boolean updateConfigOnly(Long id, String config) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.updateConfigOnly(id, config) > 0;
    }

    @Override
    public AgentSyncResponse syncData(Long agentId, String since) {
        if (agentMapper.selectById(agentId) == null) {
            throw new BusinessException("Agent not found");
        }
        
        AgentSyncResponse response = new AgentSyncResponse();
        response.setSyncTime(LocalDateTime.now());
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        List<Skill> allSkills = skillMapper.selectByAgentId(agentId);
        if (since != null && !since.isEmpty()) {
            LocalDateTime sinceDateTime = LocalDateTime.parse(since, formatter);
            allSkills = allSkills.stream()
                .filter(skill -> skill.getUpdatedAt() != null && skill.getUpdatedAt().isAfter(sinceDateTime))
                .collect(Collectors.toList());
        }
        
        List<AgentSyncResponse.SkillSyncInfo> skillInfos = allSkills.stream()
            .map(skill -> {
                AgentSyncResponse.SkillSyncInfo info = new AgentSyncResponse.SkillSyncInfo();
                info.setId(skill.getId());
                info.setName(skill.getName());
                info.setVersion(skill.getVersion());
                info.setUpdatedAt(skill.getUpdatedAt());
                return info;
            })
            .collect(Collectors.toList());
        response.setSkills(skillInfos);
        
        List<Memory> allMemories = memoryMapper.selectByAgentId(agentId);
        if (since != null && !since.isEmpty()) {
            LocalDateTime sinceDateTime = LocalDateTime.parse(since, formatter);
            allMemories = allMemories.stream()
                .filter(memory -> memory.getUpdatedAt() != null && memory.getUpdatedAt().isAfter(sinceDateTime))
                .collect(Collectors.toList());
        }
        
        List<AgentSyncResponse.MemorySyncInfo> memoryInfos = allMemories.stream()
            .map(memory -> {
                AgentSyncResponse.MemorySyncInfo info = new AgentSyncResponse.MemorySyncInfo();
                info.setId(memory.getId());
                info.setTitle(memory.getTitle());
                info.setUpdatedAt(memory.getUpdatedAt());
                return info;
            })
            .collect(Collectors.toList());
        response.setMemories(memoryInfos);
        
        return response;
    }
}
