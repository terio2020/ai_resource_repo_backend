package com.ai.repo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ai.repo.entity.SkillPublicationRequest;

@Mapper
public interface SkillPublicationRequestMapper {
    int insert(SkillPublicationRequest request);

    SkillPublicationRequest selectByTokenHash(@Param("tokenHash") String tokenHash);

    int decide(@Param("id") Long id, @Param("userId") Long userId,
               @Param("status") String status);

}
