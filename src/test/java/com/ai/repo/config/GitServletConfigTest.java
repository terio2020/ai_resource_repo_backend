package com.ai.repo.config;

import com.ai.repo.mapper.SkillRepositoryMapper;
import com.ai.repo.service.AgentService;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GitServletConfigTest {

    @Mock
    private SkillRepositoryMapper skillRepositoryMapper;

    @Mock
    private AgentService agentService;

    private GitServletConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new GitServletConfig();
        java.lang.reflect.Field field;

        field = GitServletConfig.class.getDeclaredField("gitRootPath");
        field.setAccessible(true);
        field.set(config, "/tmp/test-git-repos");

        field = GitServletConfig.class.getDeclaredField("skillRepositoryMapper");
        field.setAccessible(true);
        field.set(config, skillRepositoryMapper);

        field = GitServletConfig.class.getDeclaredField("agentService");
        field.setAccessible(true);
        field.set(config, agentService);
    }

    @Test
    void gitServlet_shouldReturnRegistrationWithCorrectUrlPattern() {
        ServletRegistrationBean<?> registration = config.gitServlet();
        assertNotNull(registration);
        assertTrue(registration.getUrlMappings().contains("/git/*"));
        assertEquals("gitServlet", registration.getServletName());
    }

    @Test
    void skillRepositoryResolver_shouldRejectPathTraversal() throws Exception {
        Constructor<?> constructor = GitServletConfig.class.getDeclaredClasses()[0]
                .getDeclaredConstructor(String.class);
        constructor.setAccessible(true);

        Object resolver = constructor.newInstance("/tmp/test-git-repos");
        java.lang.reflect.Method openMethod = resolver.getClass()
                .getDeclaredMethod("open", Object.class, String.class);

        assertThrows(InvocationTargetException.class,
                () -> openMethod.invoke(resolver, null, "../etc/passwd"));
    }

    @Test
    void skillRepositoryResolver_shouldRejectNonExistentRepo() throws Exception {
        Constructor<?> constructor = GitServletConfig.class.getDeclaredClasses()[0]
                .getDeclaredConstructor(String.class);
        constructor.setAccessible(true);

        Object resolver = constructor.newInstance("/tmp/test-git-repos");
        java.lang.reflect.Method openMethod = resolver.getClass()
                .getDeclaredMethod("open", Object.class, String.class);

        assertThrows(InvocationTargetException.class,
                () -> openMethod.invoke(resolver, null, "nonexistent_repo.git"));
    }
}
