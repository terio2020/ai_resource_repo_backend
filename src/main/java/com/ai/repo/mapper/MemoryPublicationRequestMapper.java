package com.ai.repo.mapper;

import com.ai.repo.entity.MemoryPublicationRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemoryPublicationRequestMapper {
    int insert(MemoryPublicationRequest request);
    MemoryPublicationRequest selectByTokenHash(String tokenHash);
    int decide(@Param("id") Long id, @Param("userId") Long userId, @Param("status") String status);
}
