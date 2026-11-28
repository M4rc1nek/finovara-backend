package com.finovara.authservice.settings.security.twofactor.dto;

public record AuthenticatorSetupResponse(String secret, String otpauthUrl) {
}
