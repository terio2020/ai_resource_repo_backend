package com.ai.repo.mapper;

import com.ai.repo.entity.Memory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemoryMapper {
    int insert(Memory memory);
    int update(Memory memory);
    int deleteById(Long id);
    int deleteByAgentId(@Param("agentId") Long agentId);
    Memory selectById(Long id);
    Memory selectByUid(@Param("uid") String uid);
    List<Memory> selectAll();
    List<Memory> selectByUserId(Long userId);
    List<Memory> selectByAgentId(Long agentId);
    List<Memory> selectByCategory(String category);
    List<Memory> selectByPublic(Boolean isPublic);
    List<Memory> selectByUserIdAndPublic(@Param("userId") Long userId, @Param("isPublic") Boolean isPublic);
    List<Memory> selectByAgentIdAndPublic(@Param("agentId") Long agentId, @Param("isPublic") Boolean isPublic);
    List<Memory> searchByKeyword(String keyword);
    List<Memory> searchPublicByKeyword(String keyword);
    int batchDelete(@Param("ids") List<Long> ids);
    Memory selectByUserIdAndAgentIdAndTitle(@Param("userId") Long userId, @Param("agentId") Long agentId, @Param("title") String title);
    int updateByCompositeKey(Memory memory);
    int incrementDownloadCount(@Param("id") Long id);
    int incrementLikeCount(@Param("id") Long id);
    List<com.ai.repo.dto.AgentIdCount> selectCountByAgentIds(@Param("ids") List<Long> ids);
}
