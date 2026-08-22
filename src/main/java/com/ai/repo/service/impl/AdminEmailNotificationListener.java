package com.ai.repo.service.impl;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ai.repo.event.AdminNotificationEvent;
import com.ai.repo.service.AdminEmailService;

import jakarta.annotation.Resource;

@Component
public class AdminEmailNotificationListener {

    @Resource
    private AdminEmailService adminEmailService;
    @Value("${admin.notifications.enabled:true}")
    private boolean enabled;
    @Value("${admin.notifications.events:BUG_REPORT,NEW_USER}")
    private Set<AdminNotificationEvent.Type> enabledEvents;

    @EventListener
    public void send(AdminNotificationEvent event) {
        if (!enabled || !enabledEvents.contains(event.type())) {
            return;
        }
        adminEmailService.sendToActiveAdmins(event.subject(), event.body(), event.actionUrl());
    }
}
