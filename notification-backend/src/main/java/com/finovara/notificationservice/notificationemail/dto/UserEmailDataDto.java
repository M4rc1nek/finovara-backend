package com.finovara.notificationservice.notificationemail.dto;

public record UserEmailDataDto(
        Long userId,
        String username,
        String email
) {
}

