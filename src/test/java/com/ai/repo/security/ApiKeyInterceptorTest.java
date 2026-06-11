package com.ai.repo.security;

import com.ai.repo.entity.Agent;
import com.ai.repo.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyInterceptorTest {

    @Mock
    private AgentService agentService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    private ApiKeyInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new ApiKeyInterceptor();
        java.lang.reflect.Field field;
        field = ApiKeyInterceptor.class.getDeclaredField("agentService");
        field.setAccessible(true);
        field.set(interceptor, agentService);
        field = ApiKeyInterceptor.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(interceptor, objectMapper);
    }

    @Test
    void preHandle_shouldReturnTrue_whenNotHandlerMethod() throws Exception {
        assertTrue(interceptor.preHandle(request, response, "not a handler method"));
    }

    @Test
    void preHandle_shouldReturnTrue_whenUserIdAlreadyPresent() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAuth.class)).thenReturn(mock(RequireAuth.class));
        when(request.getAttribute("userId")).thenReturn(1L);

        assertTrue(interceptor.preHandle(request, response, handlerMethod));
    }

    @Test
    void preHandle_shouldReturnTrue_whenNoAuthAnnotation() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAuth.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(ApiKeyAuth.class)).thenReturn(null);

        assertTrue(interceptor.preHandle(request, response, handlerMethod));
    }

    @Test
    void preHandle_shouldReturn401_whenNoAuthHeader() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAuth.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(ApiKeyAuth.class)).thenReturn(mock(ApiKeyAuth.class));
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("agent-auth-api-key")).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        assertFalse(interceptor.preHandle(request, response, handlerMethod));
        verify(response).setStatus(401);
    }

    @Test
    void preHandle_shouldReturn401_whenInvalidApiKey() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAuth.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(ApiKeyAuth.class)).thenReturn(mock(ApiKeyAuth.class));
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-key");
        when(agentService.findByApiKey("invalid-key")).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        assertFalse(interceptor.preHandle(request, response, handlerMethod));
        verify(response).setStatus(401);
    }

    @Test
    void preHandle_shouldReturn403_whenChallengeNotVerified() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAuth.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(ApiKeyAuth.class)).thenReturn(mock(ApiKeyAuth.class));
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-key");

        Agent agent = new Agent();
        agent.setId(1L);
        agent.setUserId(1L);
        agent.setChallengeVerified(false);
        when(agentService.findByApiKey("valid-key")).thenReturn(agent);

        when(request.getRequestURI()).thenReturn("/api/skill-repos/1");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        assertFalse(interceptor.preHandle(request, response, handlerMethod));
        verify(response).setStatus(403);
    }

    @Test
    void preHandle_shouldReturnTrue_whenValidApiKeyAndChallengeVerified() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAuth.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(ApiKeyAuth.class)).thenReturn(mock(ApiKeyAuth.class));
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-key");

        Agent agent = new Agent();
        agent.setId(1L);
        agent.setUserId(1L);
        agent.setChallengeVerified(true);
        when(agentService.findByApiKey("valid-key")).thenReturn(agent);
        when(request.getRequestURI()).thenReturn("/api/skill-repos/1");

        assertTrue(interceptor.preHandle(request, response, handlerMethod));
        verify(request).setAttribute("agentId", 1L);
        verify(request).setAttribute("userId", 1L);
    }

    @Test
    void preHandle_shouldBypassChallengeForChallengeEndpoints() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAuth.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(ApiKeyAuth.class)).thenReturn(mock(ApiKeyAuth.class));
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-key");

        Agent agent = new Agent();
        agent.setId(1L);
        agent.setUserId(1L);
        agent.setChallengeVerified(false);
        when(agentService.findByApiKey("valid-key")).thenReturn(agent);

        when(request.getRequestURI()).thenReturn("/api/auth/challenge");

        assertTrue(interceptor.preHandle(request, response, handlerMethod));
    }
}
