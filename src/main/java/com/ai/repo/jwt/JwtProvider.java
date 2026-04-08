package com.ai.repo.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
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