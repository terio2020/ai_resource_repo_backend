package com.ai.repo.service;

import com.ai.repo.util.CaptchaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() throws Exception {
        captchaService = new CaptchaService();
        java.lang.reflect.Field field = CaptchaService.class.getDeclaredField("redisTemplate");
        field.setAccessible(true);
        field.set(captchaService, redisTemplate);
    }

    private String redisKey(String id) {
        return "captcha:" + id;
    }

    /** Returns a MockedStatic that stubs CaptchaUtils to avoid loading background images. */
    private MockedStatic<CaptchaUtils> stubCaptchaUtils() {
        MockedStatic<CaptchaUtils> mocked = Mockito.mockStatic(CaptchaUtils.class, Mockito.CALLS_REAL_METHODS);
        mocked.when(CaptchaUtils::generateRandomTargetX).thenReturn(150);
        mocked.when(() -> CaptchaUtils.generatePuzzleImage(anyInt(), anyInt())).thenReturn("data:image/png;base64,STUB");
        return mocked;
    }

    // ==================== generateCaptcha ====================

    @Test
    void generateCaptcha_shouldReturnResultWithIdImageAndY() {
        try (MockedStatic<CaptchaUtils> ignored = stubCaptchaUtils()) {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();

            assertNotNull(result);
            assertNotNull(result.getId());
            assertFalse(result.getId().isEmpty());
            // UUID stripped of dashes: 32 hex chars
            assertEquals(32, result.getId().length());
            assertEquals("data:image/png;base64,STUB", result.getPuzzleImage());
            assertNotNull(result.getTargetY());
            assertTrue(result.getTargetY() >= 20 && result.getTargetY() < 80,
                    "targetY should be in range [20, 80), got: " + result.getTargetY());

            // The value stored in Redis is the targetX returned by CaptchaUtils (150)
            verify(valueOperations).set(eq(redisKey(result.getId())), eq(150), eq(5L), eq(TimeUnit.MINUTES));
        }
    }

    @Test
    void generateCaptcha_shouldGenerateUniqueIds() {
        try (MockedStatic<CaptchaUtils> ignored = stubCaptchaUtils()) {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            CaptchaService.CaptchaResult first = captchaService.generateCaptcha();
            CaptchaService.CaptchaResult second = captchaService.generateCaptcha();

            assertNotNull(first.getId());
            assertNotNull(second.getId());
            assertNotEquals(first.getId(), second.getId(), "Each captcha must have a unique id");
            verify(valueOperations, times(2)).set(anyString(), eq(150), eq(5L), eq(TimeUnit.MINUTES));
        }
    }

    @Test
    void generateCaptcha_shouldPassCaptchaUtilsTargetXToRedis() {
        try (MockedStatic<CaptchaUtils> mocked = stubCaptchaUtils()) {
            // Override targetX for this test
            mocked.when(CaptchaUtils::generateRandomTargetX).thenReturn(42);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            captchaService.generateCaptcha();

            // CaptchaUtils targetX (42) is what gets stored to Redis (the puzzle answer)
            verify(valueOperations).set(anyString(), eq(42), eq(5L), eq(TimeUnit.MINUTES));
        }
    }

    @Test
    void generateCaptcha_shouldPropagateCaptchaUtilsException() {
        try (MockedStatic<CaptchaUtils> mocked = Mockito.mockStatic(CaptchaUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(CaptchaUtils::generateRandomTargetX).thenReturn(150);
            mocked.when(() -> CaptchaUtils.generatePuzzleImage(anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("生成验证码失败"));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> captchaService.generateCaptcha());
            assertTrue(ex.getMessage().contains("生成验证码失败"));
            verify(redisTemplate, never()).opsForValue();
        }
    }

    // ==================== verifyCaptcha — input validation ====================

    @Test
    void verifyCaptcha_shouldReturnFalse_whenIdIsNull() {
        boolean result = captchaService.verifyCaptcha(null, 100);
        assertFalse(result);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void verifyCaptcha_shouldReturnFalse_whenIdIsEmpty() {
        boolean result = captchaService.verifyCaptcha("", 100);
        assertFalse(result);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void verifyCaptcha_shouldReturnFalse_whenMoveXIsNull() {
        boolean result = captchaService.verifyCaptcha("some-id", null);
        assertFalse(result);
        verify(redisTemplate, never()).opsForValue();
    }

    // ==================== verifyCaptcha — Redis lookup ====================

    @Test
    void verifyCaptcha_shouldReturnFalse_whenCaptchaNotFoundInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha:expired-id")).thenReturn(null);

        boolean result = captchaService.verifyCaptcha("expired-id", 100);
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyCaptcha_shouldReturnFalse_whenStoredValueIsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("missing"))).thenReturn(null);

        assertFalse(captchaService.verifyCaptcha("missing", 50));
    }

    // ==================== verifyCaptcha — tolerance ====================

    @Test
    void verifyCaptcha_shouldReturnTrue_whenMoveXEqualsTarget() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("exact"))).thenReturn(100);

        boolean result = captchaService.verifyCaptcha("exact", 100);
        assertTrue(result);
        verify(redisTemplate).delete(redisKey("exact"));
    }

    @Test
    void verifyCaptcha_shouldReturnTrue_whenWithinPositiveTolerance() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("cap"))).thenReturn(100);

        // max tolerance is 5
        assertTrue(captchaService.verifyCaptcha("cap", 104), "diff=4 should pass");
        verify(redisTemplate).delete(redisKey("cap"));
    }

    @Test
    void verifyCaptcha_shouldReturnTrue_whenWithinNegativeTolerance() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("cap-neg"))).thenReturn(100);

        assertTrue(captchaService.verifyCaptcha("cap-neg", 96), "diff=-4 should pass");
        verify(redisTemplate).delete(redisKey("cap-neg"));
    }

    @Test
    void verifyCaptcha_shouldReturnFalse_whenOutsidePositiveTolerance() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("over"))).thenReturn(100);

        boolean result = captchaService.verifyCaptcha("over", 110);
        assertFalse(result, "diff=10 should fail");
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyCaptcha_shouldReturnFalse_whenOutsideNegativeTolerance() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("under"))).thenReturn(100);

        boolean result = captchaService.verifyCaptcha("under", 90);
        assertFalse(result, "diff=-10 should fail");
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyCaptcha_shouldReturnFalse_atToleranceBoundaryPlusOne() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("edge"))).thenReturn(100);

        // diff=6 is just outside the <=5 tolerance
        boolean result = captchaService.verifyCaptcha("edge", 106);
        assertFalse(result, "diff=6 should fail (tolerance is <=5)");
    }

    @Test
    void verifyCaptcha_shouldConsumeTokenAfterSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("once"))).thenReturn(50);

        assertTrue(captchaService.verifyCaptcha("once", 50));
        verify(redisTemplate).delete(redisKey("once"));
    }

    @Test
    void verifyCaptcha_shouldNotConsumeTokenAfterFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey("keep"))).thenReturn(50);

        captchaService.verifyCaptcha("keep", 200);
        verify(redisTemplate, never()).delete(anyString());
    }

    // ==================== CaptchaResult ====================

    @Test
    void captchaResult_shouldExposeAllFields() {
        CaptchaService.CaptchaResult result =
                new CaptchaService.CaptchaResult("abc123", "data:image/png;base64,...", 35);

        assertEquals("abc123", result.getId());
        assertEquals("data:image/png;base64,...", result.getPuzzleImage());
        assertEquals(35, result.getTargetY().intValue());
    }

    @Test
    void captchaResult_shouldAllowNulls() {
        CaptchaService.CaptchaResult result = new CaptchaService.CaptchaResult(null, null, null);
        assertNull(result.getId());
        assertNull(result.getPuzzleImage());
        assertNull(result.getTargetY());
    }
}