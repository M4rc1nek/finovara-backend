package com.finovara.contracts.event.notification;

public record SendEmailEvent(
        String to,
        String username,
        String email,
        String subject,
        String templateName
) {
}