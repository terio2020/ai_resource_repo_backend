package com.ai.repo.mapper;

import com.ai.repo.entity.ProfileMemoryItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProfileMemoryItemMapper {
    int upsert(ProfileMemoryItem item);
    int retract(@Param("memoryId") Long memoryId, @Param("itemKey") String itemKey);
    List<ProfileMemoryItem> selectByUserId(@Param("userId") Long userId);
    List<ProfileMemoryItem> selectByMemoryId(@Param("memoryId") Long memoryId);
    int reconcileConflicts(@Param("userId") Long userId);
}
