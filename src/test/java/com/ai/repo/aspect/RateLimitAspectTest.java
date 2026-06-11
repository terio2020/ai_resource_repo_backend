package com.ai.repo.aspect;

import com.ai.repo.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new RateLimitAspect();
        java.lang.reflect.Field field = RateLimitAspect.class.getDeclaredField("stringRedisTemplate");
        field.setAccessible(true);
        field.set(aspect, stringRedisTemplate);

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void around_withoutAnnotation_shouldProceed() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(
                RateLimitAspectTest.class.getDeclaredMethod("methodWithoutAnnotation"));

        Object result = new Object();
        when(joinPoint.proceed()).thenReturn(result);

        assertSame(result, aspect.around(joinPoint));
    }

    @Test
    void around_withinLimit_shouldProceed() throws Throwable {
        Method annotatedMethod = RateLimitAspectTest.class.getDeclaredMethod("annotatedMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(annotatedMethod);
        when(joinPoint.getTarget()).thenReturn(this);

        when(valueOperations.increment(anyString())).thenReturn(1L);

        Object result = new Object();
        when(joinPoint.proceed()).thenReturn(result);

        assertSame(result, aspect.around(joinPoint));
    }

    @Test
    void around_exceedingLimit_shouldThrow() throws Throwable {
        Method annotatedMethod = RateLimitAspectTest.class.getDeclaredMethod("annotatedMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(annotatedMethod);
        when(joinPoint.getTarget()).thenReturn(this);

        when(valueOperations.increment(anyString())).thenReturn(61L);

        assertThrows(BusinessException.class, () -> aspect.around(joinPoint));
        verify(joinPoint, never()).proceed();
    }

    @SuppressWarnings("unused")
    public void methodWithoutAnnotation() {}

    @SuppressWarnings("unused")
    @RateLimit(value = 60, period = 60)
    public void annotatedMethod() {}
}
