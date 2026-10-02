package com.finovara.contracts.util.notification.event;

import com.finovara.contracts.util.notification.ActionEmailEventType;

import java.util.Map;

public record SendEmailEvent(
        Long userId,
        String username,
        String email,
        ActionEmailEventType eventType,
        Map<String, String> placeholders
) {
}