package com.ai.repo.service.impl;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.ai.repo.entity.User;
import com.ai.repo.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEmailServiceImplTest {
    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private UserMapper userMapper;
    private AdminEmailServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AdminEmailServiceImpl();
        inject("javaMailSender", javaMailSender);
        inject("userMapper", userMapper);
        inject("fromEmail", "noreply@logicoma.net");
        inject("frontendUrl", "https://logicomanet.com/");
    }

    @Test
    void sendsOnlyToActiveAdminsAndIncludesActionUrl() {
        when(userMapper.selectByRole("ADMIN")).thenReturn(List.of(
                user("Admin@Example.com", "ACTIVE"), user("disabled@example.com", "DISABLED")));
        service.sendToActiveAdmins("Custom subject", "Custom body", "/admin?tab=users");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("admin@example.com", captor.getValue().getTo()[0]);
        assertTrue(captor.getValue().getSubject().contains("Custom subject"));
        assertTrue(captor.getValue().getText().contains("https://logicomanet.com/admin?tab=users"));
    }

    @Test
    void optionalActionUrlIsOmitted() {
        when(userMapper.selectByRole("ADMIN")).thenReturn(List.of(user("admin@example.com", "ACTIVE")));
        service.sendToActiveAdmins("Notice", "Body only", null);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("Body only", captor.getValue().getText());
        assertFalse(captor.getValue().getText().contains("查看详情"));
    }

    @Test
    void mailFailureDoesNotEscapeService() {
        when(userMapper.selectByRole("ADMIN")).thenReturn(List.of(user("admin@example.com", "ACTIVE")));
        doThrow(new RuntimeException("smtp unavailable"))
                .when(javaMailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        service.sendToActiveAdmins("Notice", "Body", null);
    }

    @Test
    void sendsOnlyToSpecifiedActiveAdminOwner() {
        User owner = user("owner@example.com", "ACTIVE");
        owner.setRole("ADMIN");
        when(userMapper.selectById(42L)).thenReturn(owner);
        service.sendToAdminOwner(42L, "Agent proposal", "Review required", "/admin");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("owner@example.com", captor.getValue().getTo()[0]);
        assertTrue(captor.getValue().getSubject().contains("Agent proposal"));
    }

    private User user(String email, String status) {
        User user = new User();
        user.setEmail(email);
        user.setStatus(status);
        return user;
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = AdminEmailServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }
}
