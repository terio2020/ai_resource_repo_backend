package com.ai.repo.jwt;

import com.ai.repo.entity.Agent;
import com.ai.repo.service.AgentService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtProvider jwtProvider;

    @Resource
    private AgentService agentService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                if (isJwtFormat(token)) {
                    Long userId = jwtProvider.validateAccessToken(token);
                    if (userId != null) {
                        setAuthentication(request, userId.toString(), userId, null);
                        log.debug("User authenticated: {}", userId);
                    } else {
                        log.warn("Invalid JWT token provided");
                    }
                } else {
                    // Try API key authentication
                    Agent agent = agentService.findByApiKey(token);
                    if (agent != null) {
                        setAuthentication(request, agent.getUserId().toString(), agent.getUserId(), agent.getId());
                        log.debug("Agent authenticated: {} (user: {})", agent.getId(), agent.getUserId());
                    }
                }
            } else {
                // Try agent-auth-api-key header
                String apiKeyHeader = request.getHeader("agent-auth-api-key");
                if (StringUtils.hasText(apiKeyHeader)) {
                    Agent agent = agentService.findByApiKey(apiKeyHeader);
                    if (agent != null) {
                        setAuthentication(request, agent.getUserId().toString(), agent.getUserId(), agent.getId());
                        log.debug("Agent authenticated via header: {} (user: {})", agent.getId(), agent.getUserId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, String username, Long userId, Long agentId) {
        UserDetails userDetails = User.builder()
                .username(username)
                .password("")
                .authorities(new ArrayList<>())
                .build();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setAttribute("userId", userId);
        if (agentId != null) {
            request.setAttribute("agentId", agentId);
        }
    }

    private boolean isJwtFormat(String token) {
        int dotCount = 0;
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == '.') {
                dotCount++;
            }
        }
        return dotCount == 2;
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(JwtConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}