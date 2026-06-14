package com.finovara.authbackend.security.oauth2.dto;

public record OAuth2LoginResponseDto(
        Long id,
        String username,
        String email,
        String profileImageUrl,
        String jwtToken
) {
}
