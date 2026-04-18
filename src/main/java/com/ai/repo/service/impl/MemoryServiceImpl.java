package com.ai.repo.service.impl;

import com.ai.repo.entity.Memory;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.MemoryMapper;
import com.ai.repo.service.MemoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryServiceImpl implements MemoryService {

    @Resource
    private MemoryMapper memoryMapper;

    @Override
    public Memory create(Memory memory) {
        memoryMapper.insert(memory);
        return memory;
    }

    @Override
    public Memory update(Memory memory) {
        if (memoryMapper.selectById(memory.getId()) == null) {
            throw new BusinessException("Memory not found");
        }
        memoryMapper.update(memory);
        return memory;
    }

    @Override
    public boolean delete(Long id) {
        if (memoryMapper.selectById(id) == null) {
            throw new BusinessException("Memory not found");
        }
        return memoryMapper.deleteById(id) > 0;
    }

    @Override
    public Memory findById(Long id) {
        return memoryMapper.selectById(id);
    }

    @Override
    public List<Memory> findAll() {
        return memoryMapper.selectAll();
    }

    @Override
    public List<Memory> findByUserId(Long userId) {
        return memoryMapper.selectByUserId(userId);
    }

    @Override
    public List<Memory> findByAgentId(Long agentId) {
        return memoryMapper.selectByAgentId(agentId);
    }

    @Override
    public List<Memory> findByCategory(String category) {
        return memoryMapper.selectByCategory(category);
    }

    @Override
    public List<Memory> findByPublic(Boolean isPublic) {
        return memoryMapper.selectByPublic(isPublic);
    }

    @Override
    public List<Memory> searchByKeyword(String keyword) {
        return memoryMapper.searchByKeyword(keyword);
    }

    @Override
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("IDs cannot be null or empty");
        }
        return memoryMapper.batchDelete(ids);
    }

    @Override
    public Memory upsert(Memory memory) {
        Memory existingMemory = memoryMapper.selectByUserIdAndAgentIdAndTitle(memory.getUserId(), memory.getAgentId(), memory.getTitle());

        if (existingMemory != null) {
            memory.setId(existingMemory.getId());
            memoryMapper.updateByCompositeKey(memory);
            return memory;
        } else {
            memoryMapper.insert(memory);
            return memory;
        }
    }

    @Override
    public boolean incrementDownloadCount(Long id) {
        Memory memory = memoryMapper.selectById(id);
        if (memory == null) {
            throw new BusinessException("Memory not found");
        }
        return memoryMapper.incrementDownloadCount(id) > 0;
    }

    @Override
    public boolean incrementLikeCount(Long id) {
        Memory memory = memoryMapper.selectById(id);
        if (memory == null) {
            throw new BusinessException("Memory not found");
        }
        return memoryMapper.incrementLikeCount(id) > 0;
    }
}
