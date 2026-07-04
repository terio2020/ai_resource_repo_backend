package com.ai.repo.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        String token = Jwts.builder()
                .claim(JwtConstants.USER_ID_CLAIM, userId)
                .claim(JwtConstants.USERNAME_CLAIM, username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        storeToken(userId, token, JwtConstants.ACCESS_TOKEN_PREFIX, accessTokenExpiration);
        return token;
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        String token = Jwts.builder()
                .claim(JwtConstants.USER_ID_CLAIM, userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        storeToken(userId, token, JwtConstants.REFRESH_TOKEN_PREFIX, refreshTokenExpiration);
        return token;
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
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

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
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

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