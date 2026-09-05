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
    int deleteGeneralByAgentId(@Param("agentId") Long agentId);
    int detachProfileByAgentId(@Param("agentId") Long agentId);
    Memory selectById(Long id);
    Memory selectByUid(@Param("uid") String uid);
    List<Memory> selectAll();
    List<Memory> selectByUserId(Long userId);
    List<Memory> selectByUserIdVisibleToAgent(@Param("userId") Long userId, @Param("agentId") Long agentId);
    List<Memory> selectByAgentId(Long agentId);
    List<Memory> selectByCategory(String category);
    List<Memory> selectByCategoryVisibleToUser(@Param("category") String category, @Param("userId") Long userId,
                                                @Param("agentId") Long agentId);
    List<Memory> selectByPublic(Boolean isPublic);
    List<Memory> selectByUserIdAndPublic(@Param("userId") Long userId, @Param("isPublic") Boolean isPublic);
    List<Memory> selectByAgentIdAndPublic(@Param("agentId") Long agentId, @Param("isPublic") Boolean isPublic);
    List<Memory> searchByKeyword(String keyword);
    List<Memory> searchPublicByKeyword(String keyword);
    int batchDelete(@Param("ids") List<Long> ids);
    int batchDeleteOwned(@Param("ids") List<Long> ids, @Param("userId") Long userId, @Param("agentId") Long agentId);
    Memory selectByUserIdAndAgentIdAndTitle(@Param("userId") Long userId, @Param("agentId") Long agentId, @Param("title") String title);
    Memory selectByUserIdAndAgentIdAndClientKey(@Param("userId") Long userId, @Param("agentId") Long agentId,
                                                 @Param("clientMemoryKey") String clientMemoryKey);
    int insertProfileIfAbsent(Memory memory);
    Memory selectProfileByKeyForUpdate(@Param("userId") Long userId, @Param("agentId") Long agentId,
                                       @Param("clientMemoryKey") String clientMemoryKey);
    int updateProfileIfRevisionOlder(Memory memory);
    List<Memory> selectProfileByUserId(@Param("userId") Long userId);
    int updateByCompositeKey(Memory memory);
    int incrementDownloadCount(@Param("id") Long id);
    int incrementLikeCount(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    List<Memory> adminSelectPage(@Param("keyword") String keyword, @Param("status") String status,
                                 @Param("limit") int limit, @Param("offset") int offset);
    Long adminCount(@Param("keyword") String keyword, @Param("status") String status);
    List<com.ai.repo.dto.AgentIdCount> selectCountByAgentIds(@Param("ids") List<Long> ids);
}
