package com.ai.repo.service;

import com.ai.repo.dto.ProfileMemoryPayload;
import com.ai.repo.dto.ProfileMemoryResponse;
import com.ai.repo.entity.Memory;

public interface ProfileMemoryService {
    Memory upsert(Memory memory, ProfileMemoryPayload profile);
    ProfileMemoryResponse findByUserId(Long userId);
}
