package com.finovara.authservice.sharedaccount.model.dto;

import com.finovara.authservice.sharedaccount.model.status.InvitationStatus;

public record InvitationResponse(
        Long id,
        Long inviterUserId,
        String inviterUsername,
        InvitationStatus status
) {
}
