package com.finovara.authservice.sharedaccount.dto;

public record InvitationResponse(
        Long id,
        Long inviterUserId,
        String inviterUsername
) {
}
