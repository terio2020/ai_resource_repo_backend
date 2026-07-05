package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.dto.AgentIdCount;
import com.ai.repo.dto.AgentResourceCounts;
import com.ai.repo.dto.AgentStatsResponse;
import com.ai.repo.dto.AgentSyncResponse;
import com.ai.repo.entity.Agent;
import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.CommentMapper;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.mapper.NotificationMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.util.ApiKeyHashUtil;
import com.ai.repo.util.AvatarUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "IDLE", "BUSY", "OFFLINE");

    @Value("${file.storage.base-path:/data/logicoma-files}")
    private String basePath;

    @Resource
    private AgentMapper agentMapper;

    @Resource
    private MemoryMapper memoryMapper;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private ApiKeyHashUtil apiKeyHashUtil;

    @Override
    public Agent create(Agent agent) {
        if (agentMapper.selectByCode(agent.getCode()) != null) {
            throw new BusinessException("Agent code already exists");
        }

        if (agent.getApiKey() != null) {
            agent.setApiKeyHash(apiKeyHashUtil.hash(agent.getApiKey()));
        }

        if (agent.getAvatar() == null || agent.getAvatar().isEmpty()) {
            try {
                Path avatarDir = Paths.get(basePath, "agents", "0");
                Files.createDirectories(avatarDir);
                String nameForAvatar = agent.getDisplayName() != null ? agent.getDisplayName() : agent.getName();
                if (nameForAvatar == null) {
                    nameForAvatar = agent.getCode();
                }
                String avatarUrl = AvatarUtil.generateDefaultAvatar(
                        (long) Math.abs(agent.hashCode()), nameForAvatar, avatarDir);
                agent.setAvatar(avatarUrl);
            } catch (Throwable e) {
                System.err.println("Failed to generate default avatar: " + e.getMessage());
            }
        }

        agentMapper.insert(agent);

        if (agent.getAvatar() != null && agent.getAvatar().contains("/0_")) {
            try {
                String fileName = agent.getAvatar().substring(agent.getAvatar().lastIndexOf('/') + 1);
                Path oldFile = Paths.get(basePath, "agents", "0", fileName);
                Files.deleteIfExists(oldFile);

                Path avatarDir = Paths.get(basePath, "agents", String.valueOf(agent.getId()));
                Files.createDirectories(avatarDir);
                String nameForAvatar = agent.getDisplayName() != null ? agent.getDisplayName() : agent.getName();
                if (nameForAvatar == null) {
                    nameForAvatar = agent.getCode();
                }
                String avatarUrl = AvatarUtil.generateDefaultAvatar(
                        agent.getId(), nameForAvatar, avatarDir);
                agent.setAvatar(avatarUrl);

                Agent updateAvatar = new Agent();
                updateAvatar.setId(agent.getId());
                updateAvatar.setAvatar(avatarUrl);
                agentMapper.update(updateAvatar);
            } catch (Throwable e) {
                System.err.println("Failed to regenerate default avatar: " + e.getMessage());
            }
        }

        return agent;
    }

    @Override
    public Agent update(Agent agent) {
        Agent existing = agentMapper.selectById(agent.getId());
        if (existing == null) {
            throw new BusinessException("Agent not found");
        }
        // Preserve api_key if not explicitly provided in update request
        // (the mapper UPDATE includes api_key = #{apiKey} which would overwrite with NULL)
        if (agent.getApiKey() == null) {
            agent.setApiKey(existing.getApiKey());
            agent.setApiKeyHash(existing.getApiKeyHash());
        } else {
            agent.setApiKeyHash(apiKeyHashUtil.hash(agent.getApiKey()));
        }
        agentMapper.update(agent);
        return agent;
    }

    @Override
    public boolean delete(Long id) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        // Cascade delete: remove all associated data before deleting the agent
        memoryMapper.deleteByAgentId(id);
        notificationMapper.deleteByAgentId(id);
        commentMapper.deleteByAgentId(id);
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
    public PageResult<Agent> findPageByUserId(Long userId, Integer page, Integer size) {
        int actualPage = page != null && page > 0 ? page : 1;
        int actualSize = size != null && size > 0 ? size : 10;
        int offset = (actualPage - 1) * actualSize;

        List<Agent> records = agentMapper.selectPageByUserId(userId, actualPage, actualSize, offset);

        Long total = agentMapper.countByUserId(userId);
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
        if (status != null && !VALID_STATUSES.contains(status.toUpperCase())) {
            throw new BusinessException(400, "Invalid status: " + status + 
                ". Valid values are: ACTIVE, IDLE, BUSY, OFFLINE");
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
    public boolean updateAvatar(Long id, String avatarUrl) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.updateAvatar(id, avatarUrl) > 0;
    }

    @Override
    public AgentSyncResponse syncData(Long agentId, String since) {
        if (agentMapper.selectById(agentId) == null) {
            throw new BusinessException("Agent not found");
        }
        
        AgentSyncResponse response = new AgentSyncResponse();
        response.setSyncTime(LocalDateTime.now());
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
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

    @Override
    public Agent findByApiKey(String apiKey) {
        String hash = apiKeyHashUtil.hash(apiKey);
        return agentMapper.selectByApiKeyHash(hash);
    }

    @Override
    public boolean updateClaimStatus(Long id, boolean isClaimed) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.updateClaimStatus(id, isClaimed) > 0;
    }

    @Override
    public boolean updateKarma(Long id, int delta) {
        if (agentMapper.selectById(id) == null) {
            throw new BusinessException("Agent not found");
        }
        return agentMapper.updateKarma(id, delta) > 0;
    }

    @Override
    public Map<Long, AgentResourceCounts> getResourceCounts(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<AgentIdCount> memoryCounts = memoryMapper.selectCountByAgentIds(agentIds);

        Map<Long, AgentResourceCounts> result = new HashMap<>();
        for (Long id : agentIds) {
            result.put(id, new AgentResourceCounts(0));
        }
        for (AgentIdCount mc : memoryCounts) {
            result.get(mc.getAgentId()).setMemoryCount(mc.getCount());
        }
        return result;
    }
}
