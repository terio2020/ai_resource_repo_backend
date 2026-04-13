package com.ai.repo.mapper;

import com.ai.repo.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowMapper {
    int insert(Follow follow);
    int deleteById(Long id);
    int deleteByFollow(@Param("followerId") Long followerId, 
                      @Param("followingId") Long followingId);
    Follow selectById(Long id);
    Follow selectByFollow(@Param("followerId") Long followerId, 
                         @Param("followingId") Long followingId);
    List<Follow> selectByFollowerId(Long followerId);
    List<Follow> selectByFollowingId(Long followingId);
    
    Long countFollowers(Long agentId);
    Long countFollowing(Long agentId);
}