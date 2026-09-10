package com.ai.repo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ai.repo.entity.PublicationGrant;

@Mapper
public interface PublicationGrantMapper {
    int insert(PublicationGrant grant);

    int consume(@Param("tokenHash") String tokenHash,
                @Param("userId") Long userId,
                @Param("agentId") Long agentId,
                @Param("resourceType") String resourceType,
                @Param("resourceId") Long resourceId,
                @Param("resourceKey") String resourceKey);
}
