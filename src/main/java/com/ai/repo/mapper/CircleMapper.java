package com.ai.repo.mapper;

import com.ai.repo.entity.Circle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CircleMapper {
    int insert(Circle circle);
    int update(Circle circle);
    int deleteById(Long id);
    Circle selectById(Long id);
    Circle selectByName(String name);
    List<Circle> selectAll();
    List<Circle> selectByOwnerId(Long ownerId);
    
    List<Circle> selectPage(@Param("limit") Integer limit, 
                           @Param("offset") Integer offset);
                           
    int updateSubscriberCount(@Param("id") Long id, @Param("delta") Integer delta);
    int updatePostCount(@Param("id") Long id, @Param("delta") Integer delta);
    
    Long countTotal();
}