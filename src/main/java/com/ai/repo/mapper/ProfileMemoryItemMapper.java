package com.ai.repo.mapper;

import com.ai.repo.entity.ProfileMemoryItem;
import com.ai.repo.dto.ProfileMemoryQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProfileMemoryItemMapper {
    int upsert(ProfileMemoryItem item);
    int retract(@Param("memoryId") Long memoryId, @Param("itemKey") String itemKey);
    List<ProfileMemoryItem> selectByUserId(@Param("userId") Long userId);
    List<ProfileMemoryItem> selectByUserIdVisibleToAgent(@Param("userId") Long userId,
                                                        @Param("agentId") Long agentId);
    List<ProfileMemoryItem> selectByUserIdVisibleToAgentQuery(@Param("userId") Long userId,
                                                             @Param("agentId") Long agentId,
                                                             @Param("query") ProfileMemoryQuery query);
    List<ProfileMemoryItem> selectByMemoryId(@Param("memoryId") Long memoryId);
    ProfileMemoryItem selectByIdForUpdate(@Param("userId") Long userId, @Param("id") Long id);
    List<ProfileMemoryItem> selectConflictsForUpdate(@Param("userId") Long userId,
                                                     @Param("selectedId") Long selectedId,
                                                     @Param("namespace") String namespace,
                                                     @Param("factKey") String factKey,
                                                     @Param("contextHash") String contextHash);
    int updateGovernedItem(ProfileMemoryItem item);
    int retractConflicts(@Param("userId") Long userId, @Param("selectedId") Long selectedId,
                         @Param("namespace") String namespace, @Param("factKey") String factKey,
                         @Param("contextHash") String contextHash);
    int reconcileConflicts(@Param("userId") Long userId);
}
