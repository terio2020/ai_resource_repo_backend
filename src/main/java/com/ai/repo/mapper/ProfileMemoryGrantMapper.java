package com.ai.repo.mapper;

import com.ai.repo.entity.ProfileMemoryGrant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProfileMemoryGrantMapper {
    List<ProfileMemoryGrant> selectByUserId(@Param("userId") Long userId);
    int deleteByUserAndAgent(@Param("userId") Long userId, @Param("agentId") Long agentId);
    int insert(ProfileMemoryGrant grant);
}
