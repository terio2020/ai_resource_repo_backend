package com.ai.repo.security;

import com.ai.repo.exception.AuthenticationException;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.ChatMessageService;
import com.ai.repo.service.CommentService;
import com.ai.repo.service.MemoryService;
import com.ai.repo.service.SkillService;
import com.ai.repo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
public class PermissionChecker {

    @Autowired
    private UserService userService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Before("@annotation(com.ai.repo.security.RequireAuth)")
    public void checkAuth(JoinPoint joinPoint) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new AuthenticationException("Authentication required");
        }
        log.debug("Authentication check passed for user: {}", userId);
    }

    @Before("@annotation(requireOwnership)")
    public void checkOwnership(JoinPoint joinPoint, RequireOwnership requireOwnership) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new AuthenticationException("Authentication required");
        }

        Long resourceId = getResourceId(joinPoint, requireOwnership.idParam());
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource ID not found in request");
        }

        Long resourceOwnerId = getResourceOwnerId(requireOwnership.resourceType(), resourceId);
        if (resourceOwnerId == null) {
            throw new IllegalArgumentException("Resource not found");
        }

        if (!resourceOwnerId.equals(currentUserId)) {
            log.warn("Ownership check failed: User {} attempted to access resource {} owned by {}", 
                    currentUserId, resourceId, resourceOwnerId);
            throw new AuthenticationException("Access denied: you don't own this resource");
        }

        log.debug("Ownership check passed for user {} on resource {}", currentUserId, resourceId);
    }

    private Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object userId = request.getAttribute("userId");
            return userId != null ? Long.valueOf(userId.toString()) : null;
        }
        return null;
    }

    private Long getResourceId(JoinPoint joinPoint, String idParam) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            String pathVariable = (String) request.getAttribute(idParam);
            if (pathVariable != null) {
                try {
                    return Long.parseLong(pathVariable);
                } catch (NumberFormatException e) {
                    log.error("Invalid resource ID format: {}", pathVariable);
                    return null;
                }
            }
            
            String paramValue = request.getParameter(idParam);
            if (paramValue != null) {
                try {
                    return Long.parseLong(paramValue);
                } catch (NumberFormatException e) {
                    log.error("Invalid resource ID parameter: {}", paramValue);
                    return null;
                }
            }
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(idParam) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        return null;
    }

    private Long getResourceOwnerId(String resourceType, Long resourceId) {
        try {
            switch (resourceType.toLowerCase()) {
                case "agent":
                    return agentService.findById(resourceId).getUserId();
                case "skill":
                    return skillService.findById(resourceId).getUserId();
                case "memory":
                    return memoryService.findById(resourceId).getUserId();
                case "comment":
                    return commentService.findById(resourceId).getUserId();
                case "chatmessage":
//                    return chatMessageService.findById(resourceId).getUserId();
                    return 0L;
                default:
                    log.error("Unknown resource type: {}", resourceType);
                    return null;
            }
        } catch (Exception e) {
            log.error("Error getting resource owner for {} {}: {}", resourceType, resourceId, e.getMessage());
            return null;
        }
    }
}