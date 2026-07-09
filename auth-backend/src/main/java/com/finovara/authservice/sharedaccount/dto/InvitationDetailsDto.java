package com.finovara.authservice.sharedaccount.dto;

public record InvitationDetailsDto(
        Long invitationId,
        Long inviterUserId,
        String inviterUsername,
        String inviterEmail,
        Long inviteeUserId,
        String inviteeUsername
) {
}