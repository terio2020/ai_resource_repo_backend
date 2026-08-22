package com.ai.repo.event;

public record AdminNotificationEvent(Type type, String subject, String body, String actionUrl) {
    public enum Type {
        BUG_REPORT,
        NEW_USER
    }
}
