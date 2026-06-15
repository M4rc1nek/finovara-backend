package com.finovara.contracts.event.user;

import java.time.LocalDateTime;

public record UserCreatedEvent(
        Long userId,
        String username,
        String email,
        LocalDateTime createdAt
) {
}
