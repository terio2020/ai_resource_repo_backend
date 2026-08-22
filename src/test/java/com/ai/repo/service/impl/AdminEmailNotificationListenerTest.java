package com.ai.repo.service.impl;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ai.repo.event.AdminNotificationEvent;
import com.ai.repo.service.AdminEmailService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminEmailNotificationListenerTest {
    @Mock
    private AdminEmailService adminEmailService;
    private AdminEmailNotificationListener listener;

    @BeforeEach
    void setUp() throws Exception {
        listener = new AdminEmailNotificationListener();
        inject("adminEmailService", adminEmailService);
        inject("enabled", true);
        inject("enabledEvents", Set.of(AdminNotificationEvent.Type.BUG_REPORT,
                AdminNotificationEvent.Type.NEW_USER));
    }

    @Test
    void enabledEventDelegatesToReusableEmailService() {
        listener.send(new AdminNotificationEvent(AdminNotificationEvent.Type.BUG_REPORT,
                "New bug", "Bug details", "/bug-reports/42"));
        verify(adminEmailService).sendToActiveAdmins("New bug", "Bug details", "/bug-reports/42");
    }

    @Test
    void disabledFeatureDoesNotSend() throws Exception {
        inject("enabled", false);
        listener.send(new AdminNotificationEvent(AdminNotificationEvent.Type.NEW_USER,
                "New user", "User details", "/admin?tab=users"));
        verify(adminEmailService, never()).sendToActiveAdmins(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = AdminEmailNotificationListener.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(listener, value);
    }
}
