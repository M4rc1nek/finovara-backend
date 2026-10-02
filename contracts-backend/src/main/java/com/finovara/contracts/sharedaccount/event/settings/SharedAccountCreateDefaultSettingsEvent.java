package com.finovara.contracts.sharedaccount.event.settings;

public record SharedAccountCreateDefaultSettingsEvent(Long inviterUserId, Long inviteeUserId) {
}
