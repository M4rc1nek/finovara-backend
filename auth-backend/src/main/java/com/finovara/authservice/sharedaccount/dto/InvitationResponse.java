package com.finovara.authservice.sharedaccount.model.dto;

public record InvitationResponse(
        Long id,
        Long inviterUserId,
        String inviterUsername
) {
}
