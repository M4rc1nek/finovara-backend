package com.finovara.corebackend.user.model;

public record UserCreatedEvent(
        Long userId,
        String username,
        String email,
        String profileImage
) {
}