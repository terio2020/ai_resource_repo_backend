package com.ai.repo.service.impl;

import com.ai.repo.entity.VerificationChallenge;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.VerificationChallengeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyChallengeServiceImplTest {

    @Mock
    private VerificationChallengeMapper challengeMapper;

    @Mock
    private AgentMapper agentMapper;

    private VerifyChallengeServiceImpl verifyChallengeService;

    @BeforeEach
    void setUp() {
        verifyChallengeService = new VerifyChallengeServiceImpl();
        try {
            java.lang.reflect.Field fieldChallenge = VerifyChallengeServiceImpl.class.getDeclaredField("challengeMapper");
            fieldChallenge.setAccessible(true);
            fieldChallenge.set(verifyChallengeService, challengeMapper);
            
            java.lang.reflect.Field fieldAgent = VerifyChallengeServiceImpl.class.getDeclaredField("agentMapper");
            fieldAgent.setAccessible(true);
            fieldAgent.set(verifyChallengeService, agentMapper);
        } catch (Exception e) {
            fail("Failed to inject mock mapper: " + e.getMessage());
        }
    }

    @Test
    void requestChallenge_shouldGenerateChallenge_whenNotLocked() {
        // Given
        Long agentId = 1L;
        when(challengeMapper.selectLockedByAgentId(agentId)).thenReturn(null);
        when(challengeMapper.insert(any())).thenReturn(1);

        // When
        VerificationChallenge result = verifyChallengeService.requestChallenge(agentId);

        // Then
        assertNotNull(result);
        assertEquals(agentId, result.getAgentId());
        assertNotNull(result.getVerificationCode());
        assertTrue(result.getVerificationCode().startsWith("verify_"));
        assertNotNull(result.getChallengeText());
        assertNotNull(result.getAnswer());
        assertEquals(0, result.getAttemptCount());
        assertEquals(3, result.getMaxAttempts());
        assertEquals("pending", result.getStatus());
        assertNotNull(result.getExpiresAt());
        verify(challengeMapper).insert(any());
    }

    @Test
    void requestChallenge_shouldThrowException_whenLocked() {
        // Given
        Long agentId = 1L;
        VerificationChallenge locked = new VerificationChallenge();
        locked.setAgentId(agentId);
        locked.setStatus("locked");
        locked.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(challengeMapper.selectLockedByAgentId(agentId)).thenReturn(locked);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            verifyChallengeService.requestChallenge(agentId);
        });
        assertTrue(exception.getMessage().contains("Too many failed attempts"));
        verify(challengeMapper, never()).insert(any());
    }

    @Test
    void verifyAnswer_shouldReturnTrue_whenAnswerCorrect() {
        // Given
        String code = "verify_test123";
        Long agentId = 1L;
        BigDecimal answer = new BigDecimal("25.0");
        
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setId(1L);
        challenge.setAgentId(agentId);
        challenge.setVerificationCode(code);
        challenge.setAnswer(new BigDecimal("25.0"));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(3);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setStatus("pending");
        
        when(challengeMapper.selectByCode(code)).thenReturn(challenge);
        when(challengeMapper.updateStatus(anyLong(), anyString())).thenReturn(1);
        when(agentMapper.updateChallengeVerified(anyLong(), anyBoolean())).thenReturn(1);

        // When
        boolean result = verifyChallengeService.verifyAnswer(code, answer, agentId);

        // Then
        assertTrue(result);
        verify(challengeMapper).updateStatus(1L, "verified");
    }

    @Test
    void verifyAnswer_shouldReturnFalse_whenAnswerIncorrect() {
        // Given
        String code = "verify_test123";
        Long agentId = 1L;
        BigDecimal wrongAnswer = new BigDecimal("100.0");
        
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setId(1L);
        challenge.setAgentId(agentId);
        challenge.setVerificationCode(code);
        challenge.setAnswer(new BigDecimal("25.0")); // Correct answer
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(3);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setStatus("pending");
        
        when(challengeMapper.selectByCode(code)).thenReturn(challenge);
        when(challengeMapper.incrementAttemptCount(anyLong())).thenReturn(1);

        // When
        boolean result = verifyChallengeService.verifyAnswer(code, wrongAnswer, agentId);

        // Then
        assertFalse(result);
        verify(challengeMapper).incrementAttemptCount(1L);
        verify(challengeMapper, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void verifyAnswer_shouldThrowException_whenChallengeExpired() {
        // Given
        String code = "verify_test123";
        Long agentId = 1L;
        BigDecimal answer = new BigDecimal("25.0");
        
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setId(1L);
        challenge.setAgentId(agentId);
        challenge.setVerificationCode(code);
        challenge.setAnswer(new BigDecimal("25.0"));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(3);
        challenge.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired
        challenge.setStatus("pending");
        
        when(challengeMapper.selectByCode(code)).thenReturn(challenge);
        when(challengeMapper.updateStatus(anyLong(), anyString())).thenReturn(1);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            verifyChallengeService.verifyAnswer(code, answer, agentId);
        });
        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    void verifyAnswer_shouldThrowException_whenMaxAttemptsReachedAfterIncrement() {
        String code = "verify_test123";
        Long agentId = 1L;
        BigDecimal answer = new BigDecimal("100.0");
        
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setId(1L);
        challenge.setAgentId(agentId);
        challenge.setVerificationCode(code);
        challenge.setAnswer(new BigDecimal("25.0"));
        challenge.setAttemptCount(3);
        challenge.setMaxAttempts(3);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setStatus("pending");
        
        when(challengeMapper.selectByCode(code)).thenReturn(challenge);
        when(challengeMapper.updateStatus(anyLong(), anyString())).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            verifyChallengeService.verifyAnswer(code, answer, agentId);
        });
        assertTrue(exception.getMessage().contains("Maximum attempts reached"));
    }

    @Test
    void verifyAnswer_shouldThrowException_whenChallengeNotFound() {
        // Given
        String code = "verify_nonexistent";
        Long agentId = 1L;
        BigDecimal answer = new BigDecimal("25.0");
        
        when(challengeMapper.selectByCode(code)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            verifyChallengeService.verifyAnswer(code, answer, agentId);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void verifyAnswer_shouldThrowException_whenChallengeBelongsToDifferentAgent() {
        // Given
        String code = "verify_test123";
        Long agentId = 1L;
        Long differentAgentId = 999L;
        BigDecimal answer = new BigDecimal("25.0");
        
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setId(1L);
        challenge.setAgentId(differentAgentId); // Different agent
        challenge.setVerificationCode(code);
        challenge.setAnswer(new BigDecimal("25.0"));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(3);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setStatus("pending");
        
        when(challengeMapper.selectByCode(code)).thenReturn(challenge);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            verifyChallengeService.verifyAnswer(code, answer, agentId);
        });
        assertTrue(exception.getMessage().contains("does not belong"));
    }

    @Test
    void isLockedOut_shouldReturnTrue_whenLockedChallengeExists() {
        // Given
        Long agentId = 1L;
        VerificationChallenge locked = new VerificationChallenge();
        locked.setAgentId(agentId);
        locked.setStatus("locked");
        locked.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        
        when(challengeMapper.selectLockedByAgentId(agentId)).thenReturn(locked);

        // When
        boolean result = verifyChallengeService.isLockedOut(agentId);

        // Then
        assertTrue(result);
    }

    @Test
    void isLockedOut_shouldReturnFalse_whenNoLockedChallenge() {
        // Given
        Long agentId = 1L;
        when(challengeMapper.selectLockedByAgentId(agentId)).thenReturn(null);

        // When
        boolean result = verifyChallengeService.isLockedOut(agentId);

        // Then
        assertFalse(result);
    }

    @Test
    void isLockedOut_shouldReturnFalse_whenLockExpired() {
        // Given
        Long agentId = 1L;
        VerificationChallenge expiredLock = new VerificationChallenge();
        expiredLock.setAgentId(agentId);
        expiredLock.setStatus("locked");
        expiredLock.setLockedUntil(LocalDateTime.now().minusMinutes(5)); // Expired
        
        when(challengeMapper.selectLockedByAgentId(agentId)).thenReturn(expiredLock);

        // When
        boolean result = verifyChallengeService.isLockedOut(agentId);

        // Then
        assertFalse(result);
    }
}