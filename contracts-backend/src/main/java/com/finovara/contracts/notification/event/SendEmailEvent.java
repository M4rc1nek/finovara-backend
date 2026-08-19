package com.finovara.contracts.event.notification;

public record SendEmailEvent(
        Long userId,
        String username,
        String email,
        String subject,
        String templateName
) {
}