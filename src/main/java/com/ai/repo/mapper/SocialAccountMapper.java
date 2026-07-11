package com.ai.repo.mapper;

import com.ai.repo.entity.SocialAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SocialAccountMapper {
    int insert(SocialAccount socialAccount);
    int update(SocialAccount socialAccount);
    int deleteById(Long id);
    int deleteByUserId(Long userId);
    SocialAccount selectById(Long id);
    SocialAccount selectByUid(@Param("uid") String uid);
    SocialAccount selectByProviderAndProviderUserId(@Param("provider") String provider, @Param("providerUserId") String providerUserId);
    SocialAccount selectByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);
    List<SocialAccount> selectByUserId(Long userId);
    List<SocialAccount> selectByProvider(String provider);
}