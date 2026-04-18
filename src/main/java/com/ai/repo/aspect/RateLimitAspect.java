package com.ai.repo.aspect;

import com.ai.repo.exception.BusinessException;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class RateLimitAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String AGENT_RATE_LIMIT_PREFIX = "agent_rate_limit:";

    @Around("@annotation(com.ai.repo.aspect.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String key = generateKey(rateLimit, joinPoint);
        long limit = rateLimit.value();
        long period = rateLimit.period();

        long current = getCurrentCount(key);
        if (current >= limit) {
            throw new BusinessException("Rate limit exceeded. Max " + limit + " requests per " + period + " seconds");
        }

        Object result = joinPoint.proceed();
        
        incrementCount(key);
        return result;
    }

    private String generateKey(RateLimit rateLimit, ProceedingJoinPoint joinPoint) {
        String pattern = rateLimit.keyPattern();
        if (pattern == null || pattern.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            return RATE_LIMIT_PREFIX + className + ":" + methodName;
        }
        return pattern;
    }

    private long getCurrentCount(String key) {
        String count = stringRedisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0L;
    }

    private void incrementCount(String key) {
        stringRedisTemplate.opsForValue().increment(key);
    }
}