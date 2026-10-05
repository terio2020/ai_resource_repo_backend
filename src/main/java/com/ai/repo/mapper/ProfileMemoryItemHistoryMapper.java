package com.ai.repo.mapper;

import com.ai.repo.entity.ProfileMemoryItemHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProfileMemoryItemHistoryMapper {
    int insert(ProfileMemoryItemHistory history);
    List<ProfileMemoryItemHistory> selectByItemId(@Param("userId") Long userId,
                                                  @Param("itemId") Long itemId);
}
