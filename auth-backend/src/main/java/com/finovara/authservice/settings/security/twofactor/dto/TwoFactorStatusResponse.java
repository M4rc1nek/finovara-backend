package com.finovara.authservice.twofactor.dto;

public record TwoFactorStatusResponse(boolean enabled, int backupCodesRemaining) {
}
