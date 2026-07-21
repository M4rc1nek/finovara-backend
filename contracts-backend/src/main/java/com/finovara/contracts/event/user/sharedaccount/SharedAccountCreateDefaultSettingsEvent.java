package com.finovara.contracts.event.user.sharedaccount;

public record SharedAccountCreateDefaultSettingsEvent(Long inviterUserId, Long inviteeUserId) {
}
