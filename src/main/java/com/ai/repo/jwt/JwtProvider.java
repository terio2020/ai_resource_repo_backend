package com.ai.repo.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtProvider {

    private static final String DEFAULT_SECRET =
        "logicoma-net-secret-key-must-be-at-least-256-bits-long-for-security-ensure-this-is-changed-in-production";
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final DefaultRedisScript<Long> ROTATE_REFRESH_SCRIPT = script("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
        redis.call('SET', KEYS[2], ARGV[4], 'PX', ARGV[5])
        return 1
        """);
    private static final DefaultRedisScript<Long> REVOKE_REPLAY_SCRIPT = script("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] or redis.call('GET', KEYS[2]) ~= ARGV[2] then return 0 end
        redis.call('DEL', KEYS[1], KEYS[3], KEYS[4])
        return 1
        """);
    private static final DefaultRedisScript<Long> ISSUE_REFRESH_ACCESS_SCRIPT = script("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3])
        redis.call('SET', KEYS[3], ARGV[4], 'PX', ARGV[3])
        return 1
        """);

    private static DefaultRedisScript<Long> script(String source) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(source);
        script.setResultType(Long.class);
        return script;
    }

    public JwtProvider(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "jwt.secret is not configured. Set the JWT_SECRET environment variable to a random string of at least "
                    + MIN_SECRET_BYTES + " bytes.");
        }
        if (secret.equals(DEFAULT_SECRET)) {
            throw new IllegalStateException(
                "jwt.secret is set to the public default value. Set JWT_SECRET to a unique, secret value.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long for HS256 security.");
        }
        log.info("JWT signing secret validated (length={} bytes).", secret.getBytes(StandardCharsets.UTF_8).length);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username) {
        String token = createAccessToken(userId, username);
        storeToken(userId, token, JwtConstants.ACCESS_TOKEN_PREFIX, accessTokenExpiration);
        return token;
    }

    private String createAccessToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        String token = Jwts.builder()
                .claim(JwtConstants.USER_ID_CLAIM, userId)
                .claim(JwtConstants.USERNAME_CLAIM, username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        return token;
    }

    /** Prevent a replay-revoked or superseded refresh family from minting a new access token. */
    public String generateAccessTokenForRefresh(Long userId, String username, String refreshToken) {
        String token = createAccessToken(userId, username);
        long expiresAt = System.currentTimeMillis() + accessTokenExpiration;
        Long stored = redisTemplate.execute(ISSUE_REFRESH_ACCESS_SCRIPT,
            List.of(JwtConstants.REFRESH_TOKEN_PREFIX + userId, JwtConstants.ACCESS_TOKEN_PREFIX + userId,
                JwtConstants.EXPIRES_PREFIX + userId),
            refreshToken, token, accessTokenExpiration, String.valueOf(expiresAt));
        return Long.valueOf(1).equals(stored) ? token : null;
    }

    public String generateRefreshToken(Long userId) {
        String token = createRefreshToken(userId, UUID.randomUUID().toString());
        storeToken(userId, token, JwtConstants.REFRESH_TOKEN_PREFIX, refreshTokenExpiration);
        return token;
    }

    private String createRefreshToken(Long userId, String family) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        String token = Jwts.builder()
                .claim(JwtConstants.USER_ID_CLAIM, userId)
                .claim(JwtConstants.REFRESH_FAMILY_CLAIM, family)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        return token;
    }

    /** Atomically consume the presented token; a replay revokes only its own active family. */
    public String rotateRefreshToken(String oldToken) {
        try {
            Claims claims = parseRefreshClaims(oldToken);
            Long userId = claims.get(JwtConstants.USER_ID_CLAIM, Long.class);
            if (userId == null) return null;
            String key = JwtConstants.REFRESH_TOKEN_PREFIX + userId;
            String usedKey = usedRefreshKey(oldToken);
            String family = claims.get(JwtConstants.REFRESH_FAMILY_CLAIM, String.class);
            String current = (String) redisTemplate.opsForValue().get(key);
            if (oldToken.equals(current)) {
                if (family == null) family = UUID.randomUUID().toString(); // legacy token
                String replacement = createRefreshToken(userId, family);
                long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remaining <= 0) return null;
                Long rotated = redisTemplate.execute(ROTATE_REFRESH_SCRIPT, List.of(key, usedKey),
                    oldToken, replacement, refreshTokenExpiration, family, remaining);
                if (Long.valueOf(1).equals(rotated)) return replacement;
            }

            String usedFamily = (String) redisTemplate.opsForValue().get(usedKey);
            if (usedFamily != null) {
                String active = (String) redisTemplate.opsForValue().get(key);
                if (active != null && usedFamily.equals(parseRefreshClaims(active)
                        .get(JwtConstants.REFRESH_FAMILY_CLAIM, String.class))) {
                    redisTemplate.execute(REVOKE_REPLAY_SCRIPT,
                        List.of(key, usedKey, JwtConstants.ACCESS_TOKEN_PREFIX + userId,
                            JwtConstants.EXPIRES_PREFIX + userId), active, usedFamily);
                    log.warn("Refresh token replay detected; revoked active token family for user {}", userId);
                }
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private Claims parseRefreshClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    private String usedRefreshKey(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return JwtConstants.USED_REFRESH_TOKEN_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void storeToken(Long userId, String token, String prefix, long expiration) {
        String key = prefix + userId;
        redisTemplate.opsForValue().set(key, token, expiration, TimeUnit.MILLISECONDS);
        
        String expiresKey = JwtConstants.EXPIRES_PREFIX + userId;
        long expiresAt = System.currentTimeMillis() + expiration;
        redisTemplate.opsForValue().set(expiresKey, String.valueOf(expiresAt), expiration, TimeUnit.MILLISECONDS);
    }

    public Long validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get(JwtConstants.USER_ID_CLAIM, Long.class);
            
            String storedToken = (String) redisTemplate.opsForValue().get(JwtConstants.ACCESS_TOKEN_PREFIX + userId);
            if (storedToken == null || !storedToken.equals(token)) {
                return null;
            }

            return userId;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.error("Invalid token: {}", e.getMessage());
            return null;
        }
    }

    public Long validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get(JwtConstants.USER_ID_CLAIM, Long.class);
            
            String storedToken = (String) redisTemplate.opsForValue().get(JwtConstants.REFRESH_TOKEN_PREFIX + userId);
            if (storedToken == null || !storedToken.equals(token)) {
                return null;
            }

            return userId;
        } catch (ExpiredJwtException e) {
            log.warn("Refresh token expired: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            return null;
        }
    }

    /** Identify a signed, unexpired refresh token without accepting it for use. */
    public Long getRefreshTokenUserId(String token) {
        try {
            return parseRefreshClaims(token).get(JwtConstants.USER_ID_CLAIM, Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public void clearTokens(Long userId) {
        redisTemplate.delete(JwtConstants.ACCESS_TOKEN_PREFIX + userId);
        redisTemplate.delete(JwtConstants.REFRESH_TOKEN_PREFIX + userId);
        redisTemplate.delete(JwtConstants.EXPIRES_PREFIX + userId);
    }

    public boolean isTokenExpired(Long userId) {
        String expiresStr = (String) redisTemplate.opsForValue().get(JwtConstants.EXPIRES_PREFIX + userId);
        if (expiresStr == null) {
            return true;
        }
        try {
            long expiresAt = Long.parseLong(expiresStr);
            return System.currentTimeMillis() > expiresAt;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
