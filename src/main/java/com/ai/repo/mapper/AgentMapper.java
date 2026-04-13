package com.ai.repo.mapper;

import com.ai.repo.dto.AgentSearchRequest;
import com.ai.repo.dto.AgentStatsResponse;
import com.ai.repo.entity.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMapper {
    int insert(Agent agent);
    int update(Agent agent);
    int deleteById(Long id);
    Agent selectById(Long id);
    Agent selectByCode(String code);
    List<Agent> selectAll();
    List<Agent> selectByUserId(Long userId);
    List<Agent> selectByStatus(String status);
    List<Agent> selectByType(String type);
    
    List<Agent> selectPage(@Param("page") Integer page, @Param("size") Integer size, @Param("offset") Integer offset);
    List<Agent> selectBySearch(AgentSearchRequest request);
    AgentStatsResponse selectStats(@Param("agentId") Long agentId);
    int updateHeartbeat(@Param("id") Long id, @Param("status") String status, @Param("lastHeartbeatAt") String lastHeartbeatAt);
    int updateStatusOnly(@Param("id") Long id, @Param("status") String status);
    int updateConfigOnly(@Param("id") Long id, @Param("config") String config);

    Agent selectByApiKey(String apiKey);
    int updateClaimStatus(@Param("id") Long id, @Param("isClaimed") boolean isClaimed);
    int updateKarma(@Param("id") Long id, @Param("delta") int delta);

    Long countTotal();
    int incrementFollowerCount(@Param("id") Long id, @Param("delta") int delta);
    int incrementFollowingCount(@Param("id") Long id, @Param("delta") int delta);
    int incrementPostsCount(@Param("id") Long id, @Param("delta") int delta);
    int incrementCommentsCount(@Param("id") Long id, @Param("delta") int delta);
}
