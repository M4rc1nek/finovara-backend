package com.finovara.authservice.settings.security.twofactor.dto;

public record TwoFactorLoginResponseDto(
        Long id,
        String username,
        String email,
        String profileImageUrl,
        boolean passwordSet,
        String jwtToken
) {
}
