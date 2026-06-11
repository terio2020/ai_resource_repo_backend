package com.ai.repo.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new JwtAuthenticationFilter();
        java.lang.reflect.Field field = JwtAuthenticationFilter.class.getDeclaredField("jwtProvider");
        field.setAccessible(true);
        field.set(filter, jwtProvider);
    }

    @Test
    void doFilterInternal_withValidToken_shouldSetAuthenticationAndProceed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtProvider.validateAccessToken("valid-token")).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute("userId", 1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidToken_shouldReturn401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtProvider.validateAccessToken("invalid-token")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoToken_shouldProceed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtProvider, never()).validateAccessToken(anyString());
    }

    @Test
    void doFilterInternal_whenExceptionThrown_shouldReturn401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtProvider.validateAccessToken("some-token")).thenThrow(new RuntimeException("Unexpected error"));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }
}
