package com.ai.repo.playground.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ai.repo.exception.BusinessException;

/** Shared limit for anonymous result reads, including guessed tokens. */
@Component
public class PlaygroundPublicRateLimiter {
    private static final int GLOBAL_PER_MINUTE = 600;
    private static final int TOKEN_PER_MINUTE = 90;
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
            "local count=redis.call('INCR',KEYS[1]); "
            + "if count==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return count", Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    @Autowired
    public PlaygroundPublicRateLimiter(StringRedisTemplate redis) {
        this(redis, Clock.systemUTC());
    }

    PlaygroundPublicRateLimiter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    public void check(String token) {
        long minute = clock.instant().getEpochSecond() / 60;
        // The global bucket is checked first so rotating guessed tokens cannot
        // bypass the database protection. Hashing keeps bearer links out of keys.
        long global = increment("playground:public:global:" + minute);
        if (global > GLOBAL_PER_MINUTE) throw new BusinessException(429, "SHARE_RATE_LIMITED");
        long perToken = increment("playground:public:token:" + minute + ":" + digest(token));
        if (perToken > TOKEN_PER_MINUTE) throw new BusinessException(429, "SHARE_RATE_LIMITED");
    }

    private long increment(String key) {
        Long count = redis.execute(INCREMENT, List.of(key), "120");
        if (count == null) throw new BusinessException(503, "SHARE_RATE_LIMIT_UNAVAILABLE");
        return count;
    }

    private String digest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }
}
