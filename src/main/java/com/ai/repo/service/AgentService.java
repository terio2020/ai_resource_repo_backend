package com.ai.repo.service;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.dto.AgentStatsResponse;
import com.ai.repo.dto.AgentSyncResponse;
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
    PageResult<Agent> findPage(Integer page, Integer size);
    PageResult<Agent> findPageByUserId(Long userId, Integer page, Integer size);
    List<Agent> findBySearch(AgentSearchRequest request);
    AgentStatsResponse getStats(Long agentId);
    boolean updateHeartbeat(Long id, String status, String lastHeartbeatAt);
    boolean updateStatusOnly(Long id, String status);
    boolean updateConfigOnly(Long id, String config);
    AgentSyncResponse syncData(Long agentId, String since);

    Agent findByApiKey(String apiKey);

    boolean updateClaimStatus(Long id, boolean isClaimed);

    boolean updateKarma(Long id, int delta);

    void incrementFollowerCount(Long agentId, int delta);

    void incrementFollowingCount(Long agentId, int delta);

    void incrementPostsCount(Long agentId, int delta);

    void incrementCommentsCount(Long agentId, int delta);

    void updateFollowCounts(Long followerId, Long followingId);
}
