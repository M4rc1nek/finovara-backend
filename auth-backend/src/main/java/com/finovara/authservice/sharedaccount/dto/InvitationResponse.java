package com.finovara.authservice.sharedaccount.dto;

import java.time.LocalDateTime;

public record InvitationResponse(
        Long id,
        Long inviterUserId,
        String inviterUsername,
        LocalDateTime expiresAt
) {
}
