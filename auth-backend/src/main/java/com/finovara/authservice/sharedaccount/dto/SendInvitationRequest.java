package com.finovara.authservice.sharedaccount.dto;

public record SendInvitationRequest(
        Long inviterUserId,
        Long inviteeUserId
) {
}
