package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminAgentResponse;
import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.entity.Agent;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.service.AdminAgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminAgentServiceImpl implements AdminAgentService {

    @Resource
    private AgentMapper agentMapper;

    @Override
    public PageResult<AdminAgentResponse> listAgents(Integer page, Integer size, String keyword, String status, String type) {
        int actualPage = page != null && page > 0 ? page : 1;
        int actualSize = size != null && size > 0 ? size : 10;
        String escaped = escapeLike(keyword);

        AgentSearchRequest request = new AgentSearchRequest();
        request.setName(escaped);
        request.setStatus(status);
        request.setType(type);
        request.setPage(actualPage);
        request.setSize(actualSize);

        List<Agent> records = agentMapper.selectBySearch(request);
        List<AdminAgentResponse> items = records.stream().map(this::toResponse).collect(Collectors.toList());
        Long total = agentMapper.adminCount(escaped, status, type);

        return new PageResult<>(items, total, (long) actualPage, (long) actualSize);
    }

    @Override
    public void updateStatus(Long operatorId, Long agentId, String status) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BusinessException(404, "Agent not found");
        }
        agentMapper.updateStatusOnly(agentId, status);
        log.warn("[AUDIT] admin={} action=updateAgentStatus targetId={} value={}", operatorId, agentId, status);
    }

    private AdminAgentResponse toResponse(Agent agent) {
        AdminAgentResponse resp = new AdminAgentResponse();
        resp.setId(agent.getId());
        resp.setUid(agent.getUid());
        resp.setUserId(agent.getUserId());
        resp.setName(agent.getName());
        resp.setCode(agent.getCode());
        resp.setStatus(agent.getStatus());
        resp.setType(agent.getType());
        resp.setSyncEnabled(agent.getSyncEnabled());
        resp.setLastSyncAt(agent.getLastSyncAt());
        resp.setLastHeartbeatAt(agent.getLastHeartbeatAt());
        resp.setCreatedAt(agent.getCreatedAt());
        resp.setDisplayName(agent.getDisplayName());
        resp.setDescription(agent.getDescription());
        resp.setAvatar(agent.getAvatar());
        resp.setIsClaimed(agent.getIsClaimed());
        resp.setKarma(agent.getKarma());
        resp.setTimezone(agent.getTimezone());
        return resp;
    }

    private String escapeLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return keyword;
        }
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
