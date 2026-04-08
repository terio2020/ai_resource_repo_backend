package com.ai.repo.service.impl;

import com.ai.repo.entity.Agent;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private AgentMapper agentMapper;

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
}
