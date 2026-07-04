package com.ai.repo.security;

import com.ai.repo.entity.Agent;
import com.ai.repo.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Resource
    private AgentService agentService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        boolean hasRequireAuth = handlerMethod.getMethodAnnotation(RequireAuth.class) != null;
        boolean hasApiKeyAuth = handlerMethod.getMethodAnnotation(ApiKeyAuth.class) != null;

        if (!hasRequireAuth && !hasApiKeyAuth) {
            return true;
        }

        Object userId = request.getAttribute("userId");
        Object agentId = request.getAttribute("agentId");

        if (hasApiKeyAuth) {
            if (agentId != null) {
                return true;
            }
            if (userId != null) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"Agent API key required for this endpoint\"}");
                return false;
            }
            return authenticateByApiKey(request, response);
        }

        if (userId != null) {
            return true;
        }
        if (agentId != null) {
            return true;
        }
        return authenticateByApiKey(request, response);
    }

    private boolean authenticateByApiKey(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String authHeader = request.getHeader("Authorization");
        String apiKeyHeader = request.getHeader("agent-auth-api-key");

        String apiKey = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        } else if (apiKeyHeader != null) {
            apiKey = apiKeyHeader;
        } else {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Missing authentication\"}");
            return false;
        }

        Agent agent = agentService.findByApiKey(apiKey);
        if (agent == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid API Key\"}");
            return false;
        }

        request.setAttribute("agentId", agent.getId());
        request.setAttribute("userId", agent.getUserId());

        String requestPath = request.getRequestURI();
        boolean isChallengeEndpoint = requestPath.equals("/api/auth/challenge")
            || requestPath.equals("/api/auth/challenge/verify")
            || requestPath.equals("/api/auth/challenge/status");

        if (!isChallengeEndpoint && !Boolean.TRUE.equals(agent.getChallengeVerified())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"Challenge verification required\"}");
            return false;
        }

        return true;
    }
}