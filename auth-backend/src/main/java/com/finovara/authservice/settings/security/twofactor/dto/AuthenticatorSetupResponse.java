package com.finovara.authservice.twofactor.dto;

public record AuthenticatorSetupResponse(String secret, String otpauthUrl) {
}
