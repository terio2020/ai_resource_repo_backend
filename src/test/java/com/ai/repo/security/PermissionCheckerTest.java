package com.ai.repo.security;

import com.ai.repo.entity.Agent;
import com.ai.repo.entity.Comment;
import com.ai.repo.entity.Memory;
import com.ai.repo.entity.Skill;
import com.ai.repo.entity.User;
import com.ai.repo.exception.AuthenticationException;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.CommentService;
import com.ai.repo.service.MemoryService;
import com.ai.repo.service.SkillService;
import com.ai.repo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionCheckerTest {

    @Mock
    private UserService userService;

    @Mock
    private AgentService agentService;

    @Mock
    private SkillService skillService;

    @Mock
    private MemoryService memoryService;

    @Mock
    private CommentService commentService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest request;

    private PermissionChecker permissionChecker;

    @BeforeEach
    void setUp() throws Exception {
        permissionChecker = new PermissionChecker();
        java.lang.reflect.Field field;
        field = PermissionChecker.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(permissionChecker, userService);
        field = PermissionChecker.class.getDeclaredField("agentService");
        field.setAccessible(true);
        field.set(permissionChecker, agentService);
        field = PermissionChecker.class.getDeclaredField("skillService");
        field.setAccessible(true);
        field.set(permissionChecker, skillService);
        field = PermissionChecker.class.getDeclaredField("memoryService");
        field.setAccessible(true);
        field.set(permissionChecker, memoryService);
        field = PermissionChecker.class.getDeclaredField("commentService");
        field.setAccessible(true);
        field.set(permissionChecker, commentService);
    }

    @Test
    void checkAuth_shouldPass_whenUserIdPresent() {
        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(new ServletRequestAttributes(request));
            when(request.getAttribute("userId")).thenReturn(1L);

            assertDoesNotThrow(() -> permissionChecker.checkAuth(joinPoint));
        }
    }

    @Test
    void checkAuth_shouldThrow_whenUserIdNull() {
        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(new ServletRequestAttributes(request));
            when(request.getAttribute("userId")).thenReturn(null);

            assertThrows(AuthenticationException.class, () -> permissionChecker.checkAuth(joinPoint));
        }
    }

    @Test
    void checkAuth_shouldThrow_whenNoRequestAttributes() {
        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            assertThrows(AuthenticationException.class, () -> permissionChecker.checkAuth(joinPoint));
        }
    }

    @Test
    void checkOwnership_shouldPass_whenUserIdMatches() throws Exception {
        RequireOwnership requireOwnership = mock(RequireOwnership.class);
        when(requireOwnership.resourceType()).thenReturn("skill");
        when(requireOwnership.idParam()).thenReturn("skillId");

        Method testMethod = PermissionCheckerTest.class.getDeclaredMethod("dummyMethod", Long.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(new ServletRequestAttributes(request));
            when(request.getAttribute("userId")).thenReturn(1L);
            when(request.getAttribute("agentId")).thenReturn(null);

            Skill skill = new Skill();
            skill.setUserId(1L);
            when(skillService.findById(1L)).thenReturn(skill);

            assertDoesNotThrow(() -> permissionChecker.checkOwnership(joinPoint, requireOwnership));
        }
    }

    @Test
    void checkOwnership_shouldThrow403_whenUserIdMismatch() throws Exception {
        RequireOwnership requireOwnership = mock(RequireOwnership.class);
        when(requireOwnership.resourceType()).thenReturn("skill");
        when(requireOwnership.idParam()).thenReturn("skillId");

        Method testMethod = PermissionCheckerTest.class.getDeclaredMethod("dummyMethod", Long.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(new ServletRequestAttributes(request));
            when(request.getAttribute("userId")).thenReturn(1L);
            when(request.getAttribute("agentId")).thenReturn(null);

            Skill skill = new Skill();
            skill.setUserId(2L);
            when(skillService.findById(1L)).thenReturn(skill);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> permissionChecker.checkOwnership(joinPoint, requireOwnership));
            assertEquals(403, ex.getCode());
        }
    }

    @Test
    void checkOwnership_shouldThrowAuth_whenBothNull() {
        RequireOwnership requireOwnership = mock(RequireOwnership.class);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(new ServletRequestAttributes(request));
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getAttribute("agentId")).thenReturn(null);

            assertThrows(AuthenticationException.class,
                    () -> permissionChecker.checkOwnership(joinPoint, requireOwnership));
        }
    }

    @SuppressWarnings("unused")
    public void dummyMethod(Long skillId) {}
}
