package com.finovara.authservice.sharedaccount.dto;

public record ExpiredInvitationDto(Long id, Long inviterUserId, String inviteeUsername) {
}