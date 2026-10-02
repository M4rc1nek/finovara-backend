package com.finovara.contracts.util.email.event;

import com.finovara.contracts.notification.ActionEmailEventType;

import java.util.Map;

public record SendEmailEvent(
        Long userId,
        String username,
        String email,
        ActionEmailEventType eventType,
        Map<String, String> placeholders
) {
}