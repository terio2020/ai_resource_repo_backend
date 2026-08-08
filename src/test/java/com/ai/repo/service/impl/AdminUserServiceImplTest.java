package com.ai.repo.service.impl;

import com.ai.repo.common.PageResult;
import com.ai.repo.dto.AdminUserResponse;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.AgentMapper;
import com.ai.repo.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AgentMapper agentMapper;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() throws Exception {
        adminUserService = new AdminUserServiceImpl();
        setField("userMapper", userMapper);
        setField("agentMapper", agentMapper);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = AdminUserServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(adminUserService, value);
    }

    private User user(long id, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setUid("uid-" + id);
        user.setUsername("user" + id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    @Test
    void listUsers_shouldMapAndPage() {
        User u = user(1L, "USER", "ACTIVE");
        u.setEmail("a@b.com");
        when(userMapper.adminSelectPage(eq("key"), eq("USER"), eq("ACTIVE"), eq(10), eq(20)))
                .thenReturn(Collections.singletonList(u));
        when(userMapper.adminCount(eq("key"), eq("USER"), eq("ACTIVE"))).thenReturn(42L);

        PageResult<AdminUserResponse> result = adminUserService.listUsers(3, 10, "key", "USER", "ACTIVE");

        assertEquals(1, result.getRecords().size());
        assertEquals(42L, result.getTotal());
        assertEquals(3L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(5L, result.getPages());
        AdminUserResponse item = result.getRecords().get(0);
        assertEquals(1L, item.getId());
        assertEquals("user1", item.getUsername());
        assertEquals("USER", item.getRole());
        assertEquals("ACTIVE", item.getStatus());
    }

    @Test
    void listUsers_shouldEscapeLikeWildcards() {
        when(userMapper.adminSelectPage(eq("50\\%\\_x\\\\"), any(), any(), eq(10), eq(0)))
                .thenReturn(Collections.emptyList());
        when(userMapper.adminCount(eq("50\\%\\_x\\\\"), any(), any())).thenReturn(0L);

        adminUserService.listUsers(1, 10, "50%_x\\", null, null);

        verify(userMapper).adminSelectPage(eq("50\\%\\_x\\\\"), any(), any(), eq(10), eq(0));
        verify(userMapper).adminCount(eq("50\\%\\_x\\\\"), any(), any());
    }

    @Test
    void listUsers_shouldDefaultPaging() {
        when(userMapper.adminSelectPage(isNull(), isNull(), isNull(), eq(10), eq(0)))
                .thenReturn(Collections.emptyList());
        when(userMapper.adminCount(isNull(), isNull(), isNull())).thenReturn(0L);

        adminUserService.listUsers(null, null, null, null, null);

        verify(userMapper).adminSelectPage(isNull(), isNull(), isNull(), eq(10), eq(0));
    }

    @Test
    void updateRole_shouldUpdatePreservingStatus() {
        User u = user(2L, "USER", "ACTIVE");
        when(userMapper.selectById(2L)).thenReturn(u);

        adminUserService.updateRole(1L, 2L, "ADMIN");

        verify(userMapper).updateRoleAndStatus(2L, "ADMIN", "ACTIVE");
    }

    @Test
    void updateRole_shouldRejectSelfChange() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateRole(1L, 1L, "ADMIN"));
        assertEquals(400, ex.getCode());
        verify(userMapper, never()).updateRoleAndStatus(any(), any(), any());
    }

    @Test
    void updateRole_shouldRejectMissingUser() {
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateRole(1L, 99L, "ADMIN"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateStatus_disable_shouldCascadeToAgents() {
        User u = user(2L, "USER", "ACTIVE");
        when(userMapper.selectById(2L)).thenReturn(u);

        adminUserService.updateStatus(1L, 2L, "DISABLED");

        verify(agentMapper).disableByUserId(2L);
        verify(userMapper).updateRoleAndStatus(2L, "USER", "DISABLED");
    }

    @Test
    void updateStatus_reactivate_shouldNotCascade() {
        User u = user(2L, "USER", "DISABLED");
        when(userMapper.selectById(2L)).thenReturn(u);

        adminUserService.updateStatus(1L, 2L, "ACTIVE");

        verify(agentMapper, never()).disableByUserId(any());
        verify(userMapper).updateRoleAndStatus(2L, "USER", "ACTIVE");
    }

    @Test
    void updateStatus_shouldRejectDisablingSelf() {
        User u = user(1L, "ADMIN", "ACTIVE");
        when(userMapper.selectById(1L)).thenReturn(u);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateStatus(1L, 1L, "DISABLED"));
        assertEquals(400, ex.getCode());
        verify(userMapper, never()).updateRoleAndStatus(any(), any(), any());
        verify(agentMapper, never()).disableByUserId(any());
    }

    @Test
    void updateStatus_shouldRejectDisablingLastActiveAdmin() {
        User u = user(2L, "ADMIN", "ACTIVE");
        when(userMapper.selectById(2L)).thenReturn(u);
        when(userMapper.countActiveAdmins()).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateStatus(1L, 2L, "DISABLED"));
        assertEquals(400, ex.getCode());
        verify(userMapper, never()).updateRoleAndStatus(any(), any(), any());
        verify(agentMapper, never()).disableByUserId(any());
    }

    @Test
    void updateStatus_shouldAllowDisablingNonLastActiveAdmin() {
        User u = user(2L, "ADMIN", "ACTIVE");
        when(userMapper.selectById(2L)).thenReturn(u);
        when(userMapper.countActiveAdmins()).thenReturn(2L);

        adminUserService.updateStatus(1L, 2L, "DISABLED");

        verify(agentMapper).disableByUserId(2L);
        verify(userMapper).updateRoleAndStatus(2L, "ADMIN", "DISABLED");
    }

    @Test
    void updateStatus_shouldRejectMissingUser() {
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateStatus(1L, 99L, "DISABLED"));
        assertEquals(404, ex.getCode());
    }
}
