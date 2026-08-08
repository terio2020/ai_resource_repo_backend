package com.ai.repo.config;

import com.ai.repo.entity.User;
import com.ai.repo.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserMapper userMapper;

    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AdminBootstrapRunner();
        ReflectionTestUtils.setField(runner, "userMapper", userMapper);
    }

    @Test
    void run_emptyConfig_shouldSkip() {
        ReflectionTestUtils.setField(runner, "bootstrapEmails", "");

        runner.run();

        verify(userMapper, never()).selectByEmail(anyString());
        verify(userMapper, never()).updateRoleAndStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void run_nullConfig_shouldSkip() {
        ReflectionTestUtils.setField(runner, "bootstrapEmails", null);

        runner.run();

        verify(userMapper, never()).selectByEmail(anyString());
        verify(userMapper, never()).updateRoleAndStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void run_emailNotFound_shouldWarnAndSkip() {
        ReflectionTestUtils.setField(runner, "bootstrapEmails", "missing@example.com");
        when(userMapper.selectByEmail("missing@example.com")).thenReturn(null);

        runner.run();

        verify(userMapper, never()).updateRoleAndStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void run_existingUser_shouldPromoteToAdmin() {
        User user = new User();
        user.setId(1L);
        user.setRole("USER");
        user.setStatus("ACTIVE");
        ReflectionTestUtils.setField(runner, "bootstrapEmails", "admin@example.com");
        when(userMapper.selectByEmail("admin@example.com")).thenReturn(user);

        runner.run();

        verify(userMapper).updateRoleAndStatus(1L, "ADMIN", "ACTIVE");
    }

    @Test
    void run_alreadyAdmin_shouldBeIdempotent() {
        User user = new User();
        user.setId(1L);
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        ReflectionTestUtils.setField(runner, "bootstrapEmails", "admin@example.com");
        when(userMapper.selectByEmail("admin@example.com")).thenReturn(user);

        runner.run();

        verify(userMapper, never()).updateRoleAndStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void run_multipleEmails_shouldProcessEach() {
        User admin1 = new User();
        admin1.setId(1L);
        admin1.setRole("USER");
        admin1.setStatus("ACTIVE");

        User admin2 = new User();
        admin2.setId(2L);
        admin2.setRole("ADMIN");
        admin2.setStatus("ACTIVE");

        ReflectionTestUtils.setField(runner, "bootstrapEmails", "a@example.com,b@example.com");
        when(userMapper.selectByEmail("a@example.com")).thenReturn(admin1);
        when(userMapper.selectByEmail("b@example.com")).thenReturn(admin2);

        runner.run();

        verify(userMapper).updateRoleAndStatus(1L, "ADMIN", "ACTIVE");
        verify(userMapper, never()).updateRoleAndStatus(2L, "ADMIN", "ACTIVE");
    }

    @Test
    void run_mapperThrows_shouldSwallowException() {
        ReflectionTestUtils.setField(runner, "bootstrapEmails", "admin@example.com");
        when(userMapper.selectByEmail("admin@example.com")).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> runner.run());
    }
}
