package com.ai.repo.mapper;

import com.ai.repo.entity.ShareLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShareLinkMapper {
    int insert(ShareLink shareLink);
    ShareLink selectByUid(@Param("uid") String uid);
    ShareLink findByToken(@Param("token") String token);
    ShareLink findBySkillAndCreator(@Param("skillId") Long skillId, @Param("createdBy") Long createdBy);
    int incrementViewCount(@Param("id") Long id);
}
