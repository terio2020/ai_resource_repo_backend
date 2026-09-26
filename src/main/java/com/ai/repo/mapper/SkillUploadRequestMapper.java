package com.ai.repo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ai.repo.entity.SkillUploadRequest;

@Mapper
public interface SkillUploadRequestMapper {
    int insert(SkillUploadRequest request);

    SkillUploadRequest selectByTokenHash(@Param("tokenHash") String tokenHash);

    int decide(@Param("id") Long id, @Param("userId") Long userId,
               @Param("status") String status);

    int claimCreate(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("agentId") Long agentId);

    int finishCreate(@Param("id") Long id, @Param("repositoryId") Long repositoryId);

    int consumePush(@Param("id") Long id, @Param("repositoryId") Long repositoryId,
                    @Param("agentId") Long agentId);
}
