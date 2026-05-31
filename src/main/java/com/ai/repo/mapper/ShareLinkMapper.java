package com.ai.repo.mapper;

import com.ai.repo.entity.ShareLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShareLinkMapper {
    int insert(ShareLink shareLink);
    ShareLink findByToken(@Param("token") String token);
    int incrementViewCount(@Param("id") Long id);
}
