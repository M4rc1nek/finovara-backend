package com.finovara.authservice.sharedaccount.model.dto;

import com.finovara.authservice.sharedaccount.model.status.InvitationStatus;

import java.time.LocalDateTime;

public record SharedAccountInvitationDto(
        Long inviterUserId,

        Long inviteeUserId,

        InvitationStatus status,

        LocalDateTime createdAt
) {
}
