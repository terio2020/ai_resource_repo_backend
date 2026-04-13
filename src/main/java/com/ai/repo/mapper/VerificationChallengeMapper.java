package com.ai.repo.mapper;

import com.ai.repo.entity.VerificationChallenge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VerificationChallengeMapper {
    int insert(VerificationChallenge challenge);
    int update(VerificationChallenge challenge);
    int deleteById(Long id);
    
    VerificationChallenge selectById(Long id);
    VerificationChallenge selectByCode(String verificationCode);
    List<VerificationChallenge> selectByAgentId(Long agentId);
    List<VerificationChallenge> selectByTarget(@Param("targetId") Long targetId,
                                               @Param("targetType") String targetType);
    List<VerificationChallenge> selectExpired();
    
    int incrementAttemptCount(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}