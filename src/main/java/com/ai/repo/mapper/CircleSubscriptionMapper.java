package com.ai.repo.mapper;

import com.ai.repo.entity.CircleSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CircleSubscriptionMapper {
    int insert(CircleSubscription subscription);
    int deleteById(Long id);
    int deleteByAgentCircle(@Param("agentId") Long agentId,
                            @Param("circleId") Long circleId);
    
    CircleSubscription selectById(Long id);
    CircleSubscription selectByAgentCircle(@Param("agentId") Long agentId,
                                          @Param("circleId") Long circleId);
    List<CircleSubscription> selectByAgentId(Long agentId);
    List<CircleSubscription> selectByCircleId(Long circleId);
    
    Long countByAgentId(Long agentId);
    Long countByCircleId(Long circleId);
}