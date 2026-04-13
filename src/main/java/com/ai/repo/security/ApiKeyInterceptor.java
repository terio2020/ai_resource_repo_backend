package com.ai.repo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ai.repo.entity.Agent;
import com.ai.repo.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

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
        if (handlerMethod.getMethodAnnotation(ApiKeyAuth.class) == null) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        String apiKeyHeader = request.getHeader("agent-auth-api-key");

        String apiKey = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        } else if (apiKeyHeader != null) {
            apiKey = apiKeyHeader;
        } else {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":401,\"message\":\"Missing API Key\",\"data\":null}");
            return false;
        }

        Agent agent = agentService.findByApiKey(apiKey);
        if (agent == null) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid API Key\",\"data\":null}");
            return false;
        }

        request.setAttribute("agentId", agent.getId());
        return true;
    }
}