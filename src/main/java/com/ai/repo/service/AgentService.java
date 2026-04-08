package com.ai.repo.service;

import com.ai.repo.entity.Agent;

import java.util.List;

public interface AgentService {
    Agent create(Agent agent);
    Agent update(Agent agent);
    boolean delete(Long id);
    Agent findById(Long id);
    Agent findByCode(String code);
    List<Agent> findAll();
    List<Agent> findByUserId(Long userId);
    List<Agent> findByStatus(String status);
    List<Agent> findByType(String type);
}
