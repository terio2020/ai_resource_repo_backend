package com.ai.repo.playground.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.ai.repo.exception.BusinessException;

class PlaygroundPublicRateLimiterTest {
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);

    @Test void checksGlobalBeforeTokenAndNeverWritesBearerTokenIntoRedisKey() {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        List<String> keys=new ArrayList<>();
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenAnswer(call -> {
            keys.add(((List<String>) call.getArgument(1)).get(0));
            return 1L;
        });
        new PlaygroundPublicRateLimiter(redis,CLOCK).check(TOKEN);
        assertEquals(2,keys.size());
        assertTrue(keys.get(0).startsWith("playground:public:global:"));
        assertTrue(keys.get(1).startsWith("playground:public:token:"));
        assertFalse(keys.get(1).contains(TOKEN));
    }

    @Test void globalCapStopsRotatingTokensBeforePerTokenOrDatabaseRead() {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(601L);
        BusinessException error=assertThrows(BusinessException.class,
                () -> new PlaygroundPublicRateLimiter(redis,CLOCK).check(TOKEN));
        assertEquals(429,error.getCode());
        verify(redis,times(1)).execute(any(RedisScript.class),anyList(),anyString());
    }

    @Test void repeatedReadsOfOneResultAreCapped() {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        AtomicLong tokenReads=new AtomicLong();
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenAnswer(call -> {
            String key=((List<String>) call.getArgument(1)).get(0);
            return key.contains(":token:") ? tokenReads.incrementAndGet() : 1L;
        });
        PlaygroundPublicRateLimiter limiter=new PlaygroundPublicRateLimiter(redis,CLOCK);
        for (int i=0;i<90;i++) limiter.check(TOKEN);
        BusinessException error=assertThrows(BusinessException.class,()->limiter.check(TOKEN));
        assertEquals(429,error.getCode());
        assertEquals(91,tokenReads.get());
    }

    @Test void redisFailureFailsClosed() {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(null);
        BusinessException error=assertThrows(BusinessException.class,
                () -> new PlaygroundPublicRateLimiter(redis,CLOCK).check(TOKEN));
        assertEquals(503,error.getCode());
    }

    @Test void productionShareServiceRequiresTheLimiterBean() {
        try (var context=new AnnotationConfigApplicationContext()) {
            context.registerBean(com.ai.repo.playground.mapper.PlaygroundMapper.class,
                    () -> mock(com.ai.repo.playground.mapper.PlaygroundMapper.class));
            context.registerBean(PlaygroundService.class,() -> mock(PlaygroundService.class));
            context.registerBean(com.fasterxml.jackson.databind.ObjectMapper.class,
                    () -> new com.fasterxml.jackson.databind.ObjectMapper());
            context.registerBean(StringRedisTemplate.class,() -> mock(StringRedisTemplate.class));
            context.register(PlaygroundPublicRateLimiter.class,PlaygroundShareService.class);
            context.refresh();
            assertNotNull(context.getBean(PlaygroundShareService.class));
            assertNotNull(context.getBean(PlaygroundPublicRateLimiter.class));
        }
    }
}
