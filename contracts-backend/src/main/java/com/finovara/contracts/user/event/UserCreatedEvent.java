package com.finovara.contracts.user.event;

import java.time.LocalDateTime;

public record UserCreatedEvent(
        Long userId,
        String username,
        String email,
        LocalDateTime createdAt
) {
}
