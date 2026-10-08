package com.ai.repo.service;

import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.dto.ProfileMemoryGovernRequest;
import com.ai.repo.dto.ProfileMemoryResponse;
import com.ai.repo.dto.ProfileMemoryQuery;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.ProfileMemoryGrant;
import com.ai.repo.entity.ProfileMemoryItem;
import com.ai.repo.entity.ProfileMemoryItemHistory;

import java.util.List;

public interface ProfileMemoryService {
    Memory upsert(Memory memory, ProfileMemoryPayload profile);
    ProfileMemoryResponse findByUserId(Long userId);
    ProfileMemoryResponse findByUserIdVisibleToAgent(Long userId, Long agentId);
    ProfileMemoryResponse findByUserIdVisibleToAgent(Long userId, Long agentId, ProfileMemoryQuery query);
    ProfileMemoryItem governItem(Long userId, Long itemId, ProfileMemoryGovernRequest request);
    List<ProfileMemoryItemHistory> findItemHistory(Long userId, Long itemId);
    List<ProfileMemoryGrant> findGrants(Long userId);
    List<ProfileMemoryGrant> replaceGrants(Long userId, Long agentId, List<String> namespaces);
}
