package com.ai.repo.config;

import com.ai.repo.entity.SkillRepository;
import com.ai.repo.mapper.SkillRepositoryMapper;
import com.ai.repo.service.AgentService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitServletConfigTest {

    @Mock
    private SkillRepositoryMapper skillRepositoryMapper;

    @Mock
    private AgentService agentService;

    private GitServletConfig config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        config = new GitServletConfig();
        java.lang.reflect.Field field;

        field = GitServletConfig.class.getDeclaredField("gitRootPath");
        field.setAccessible(true);
        field.set(config, tempDir.toAbsolutePath().toString());

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
        assertTrue(registration.getUrlMappings().contains("/api/git/*"));
        assertEquals("gitServlet", registration.getServletName());
    }

    @Test
    void skillRepositoryResolver_shouldRejectPathTraversal() throws Exception {
        Object resolver = createResolver();
        java.lang.reflect.Method openMethod = resolver.getClass()
                .getDeclaredMethod("open", Object.class, String.class);

        assertThrows(InvocationTargetException.class,
                () -> openMethod.invoke(resolver, null, "../etc/passwd"));
    }

    @Test
    void skillRepositoryResolver_shouldRejectNonExistentRepo() throws Exception {
        when(skillRepositoryMapper.selectByRepoPath(anyString())).thenReturn(null);

        Object resolver = createResolver();
        java.lang.reflect.Method openMethod = resolver.getClass()
                .getDeclaredMethod("open", Object.class, String.class);

        assertThrows(InvocationTargetException.class,
                () -> openMethod.invoke(resolver, null, "nonexistent_repo.git"));
    }

    @Test
    void skillRepositoryResolver_shouldLazyInitAndOpen_whenDbRecordExists() throws Exception {
        Path repoPath = tempDir.resolve("agent_5/my-skill.git");
        String repoPathStr = repoPath.toAbsolutePath().toString();

        SkillRepository dbRepo = new SkillRepository();
        dbRepo.setId(1L);
        dbRepo.setAgentId(5L);
        dbRepo.setRepoPath(repoPathStr);
        when(skillRepositoryMapper.selectByRepoPath(repoPathStr)).thenReturn(dbRepo);

        Object resolver = createResolver();
        java.lang.reflect.Method openMethod = resolver.getClass()
                .getDeclaredMethod("open", Object.class, String.class);

        Object repoObj = openMethod.invoke(resolver, null, "agent_5/my-skill.git");

        assertNotNull(repoObj);
        assertInstanceOf(Repository.class, repoObj);
        assertTrue(Files.exists(repoPath), "Bare repo should be created on disk");
        assertTrue(Files.exists(repoPath.resolve("HEAD")), "HEAD should exist");

        ((Repository) repoObj).close();
    }

    private Object createResolver() throws Exception {
        Class<?> resolverClass = GitServletConfig.class.getDeclaredClasses()[0];
        Constructor<?> constructor = resolverClass.getDeclaredConstructor(
                GitServletConfig.class, String.class, SkillRepositoryMapper.class);
        constructor.setAccessible(true);
        return constructor.newInstance(config, tempDir.toAbsolutePath().toString(), skillRepositoryMapper);
    }
}