package com.ai.repo.jwt;

import com.ai.repo.entity.Agent;
import com.ai.repo.entity.User;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.AgentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AgentService agentService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() throws Exception {
        filter = new JwtAuthenticationFilter();
        java.lang.reflect.Field jwtField = JwtAuthenticationFilter.class.getDeclaredField("jwtProvider");
        jwtField.setAccessible(true);
        jwtField.set(filter, jwtProvider);
        java.lang.reflect.Field agentField = JwtAuthenticationFilter.class.getDeclaredField("agentService");
        agentField.setAccessible(true);
        agentField.set(filter, agentService);
        java.lang.reflect.Field userField = JwtAuthenticationFilter.class.getDeclaredField("userMapper");
        userField.setAccessible(true);
        userField.set(filter, userMapper);
    }

    @Test
    void doFilterInternal_withValidJwt_shouldSetAuthenticationAndProceed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer header.payload.signature");
        when(jwtProvider.validateAccessToken("header.payload.signature")).thenReturn(1L);
        User active = new User();
        active.setStatus("ACTIVE");
        when(userMapper.selectById(1L)).thenReturn(active);

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute("userId", 1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withDisabledUserJwt_shouldNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer header.payload.signature");
        when(jwtProvider.validateAccessToken("header.payload.signature")).thenReturn(1L);
        User disabled = new User();
        disabled.setStatus("DISABLED");
        when(userMapper.selectById(1L)).thenReturn(disabled);

        filter.doFilterInternal(request, response, filterChain);

        verify(request, never()).setAttribute(eq("userId"), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withDeletedUserJwt_shouldNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer header.payload.signature");
        when(jwtProvider.validateAccessToken("header.payload.signature")).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(request, never()).setAttribute(eq("userId"), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenAccountLookupFails_shouldNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer header.payload.signature");
        when(jwtProvider.validateAccessToken("header.payload.signature")).thenReturn(1L);
        when(userMapper.selectById(1L)).thenThrow(new IllegalStateException("Account lookup unavailable"));

        filter.doFilterInternal(request, response, filterChain);

        verify(request, never()).setAttribute(eq("userId"), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withApiKey_shouldAuthenticateAndProceed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer api-key-without-dots");
        Agent agent = new Agent();
        agent.setId(5L);
        agent.setUserId(1L);
        when(agentService.findByApiKey("api-key-without-dots")).thenReturn(agent);

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute("userId", 1L);
        verify(request).setAttribute("agentId", 5L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withApiKeyHeader_shouldAuthenticateAndProceed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("agent-auth-api-key")).thenReturn("api-key-from-header");
        Agent agent = new Agent();
        agent.setId(3L);
        agent.setUserId(1L);
        when(agentService.findByApiKey("api-key-from-header")).thenReturn(agent);

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute("userId", 1L);
        verify(request).setAttribute("agentId", 3L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidApiKey_shouldProceedWithoutAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer unknown-api-key");
        when(agentService.findByApiKey("unknown-api-key")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(request, never()).setAttribute(eq("userId"), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoToken_shouldProceed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtProvider, never()).validateAccessToken(anyString());
    }

    @Test
    void doFilterInternal_whenExceptionThrown_shouldProceed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer header.payload.signature");
        when(jwtProvider.validateAccessToken("header.payload.signature")).thenThrow(new RuntimeException("Unexpected error"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
