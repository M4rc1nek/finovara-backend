package com.finovara.authservice.settings.security.twofactor.dto;

public record TwoFactorStatusResponse(boolean enabled, int backupCodesRemaining) {
}
