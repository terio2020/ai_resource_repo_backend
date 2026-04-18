package com.ai.repo.service;

import com.ai.repo.entity.Memory;

import java.util.List;

public interface MemoryService {
    Memory create(Memory memory);
    Memory update(Memory memory);
    boolean delete(Long id);
    Memory findById(Long id);
    List<Memory> findAll();
    List<Memory> findByUserId(Long userId);
    List<Memory> findByAgentId(Long agentId);
    List<Memory> findByCategory(String category);
    List<Memory> findByPublic(Boolean isPublic);
    List<Memory> searchByKeyword(String keyword);
    int batchDelete(List<Long> ids);
    Memory upsert(Memory memory);
    boolean incrementDownloadCount(Long id);
    boolean incrementLikeCount(Long id);
}
