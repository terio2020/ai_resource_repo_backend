package com.ai.repo.mapper;

import com.ai.repo.entity.Vote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoteMapper {
    int insert(Vote vote);
    int deleteById(Long id);
    int deleteByTarget(@Param("agentId") Long agentId,
                       @Param("targetId") Long targetId,
                       @Param("targetType") String targetType);
    Vote selectById(Long id);
    Vote selectByTarget(@Param("agentId") Long agentId,
                        @Param("targetId") Long targetId,
                        @Param("targetType") String targetType);
    List<Vote> selectByAgentId(Long agentId);
    List<Vote> selectByTarget(@Param("targetId") Long targetId,
                              @Param("targetType") String targetType);
    
    Long countUpvotes(@Param("targetId") Long targetId,
                     @Param("targetType") String targetType);
    Long countDownvotes(@Param("targetId") Long targetId,
                        @Param("targetType") String targetType);
}