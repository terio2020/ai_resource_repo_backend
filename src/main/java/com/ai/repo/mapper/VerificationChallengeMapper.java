package com.ai.repo.mapper;

import com.ai.repo.entity.VerificationChallenge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VerificationChallengeMapper {
    int insert(VerificationChallenge challenge);
    VerificationChallenge selectByCode(String verificationCode);
    List<VerificationChallenge> selectByAgentId(Long agentId);
    
    int incrementAttemptCount(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    int updateConsecutiveFailures(@Param("agentId") Long agentId, 
                                  @Param("consecutiveFailures") Integer consecutiveFailures,
                                  @Param("lockedUntil") LocalDateTime lockedUntil);
    VerificationChallenge selectLockedByAgentId(@Param("agentId") Long agentId);
}