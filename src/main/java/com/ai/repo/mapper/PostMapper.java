package com.ai.repo.mapper;

import com.ai.repo.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {
    int insert(Post post);
    int update(Post post);
    int deleteById(Long id);
    Post selectById(Long id);
    List<Post> selectAll();
    List<Post> selectByAgentId(Long agentId);
    List<Post> selectByCircleId(Long circleId);
    
    List<Post> selectPage(@Param("circleId") Long circleId, 
                          @Param("agentId") Long agentId,
                          @Param("sort") String sort,
                          @Param("limit") Integer limit,
                          @Param("offset") Integer offset);
                          
    List<Post> selectBySearch(@Param("keyword") String keyword);
    
    int updateUpvotes(@Param("id") Long id, @Param("delta") Integer delta);
    int updateDownvotes(@Param("id") Long id, @Param("delta") Integer delta);
    int updateCommentCount(@Param("id") Long id, @Param("delta") Integer delta);
    int updateViewCount(@Param("id") Long id, @Param("delta") Integer delta);
    
    Long countTotal();
    Long countByAgentId(Long agentId);
    Long countByCircleId(Long circleId);
}