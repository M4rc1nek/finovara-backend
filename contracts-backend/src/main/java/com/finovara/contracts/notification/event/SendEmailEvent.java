package com.finovara.contracts.notification.event;

import com.finovara.contracts.notification.email.ActionEmailEventType;

import java.util.Map;

public record SendEmailEvent(
        Long userId,
        String username,
        String email,
        ActionEmailEventType eventType,
        Map<String, String> placeholders
) {
}