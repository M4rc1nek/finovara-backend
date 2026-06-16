package com.finovara.authservice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginDto(
        Long id,

        String username,
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,
        String userProfileImage,

        String jwtToken
) {
}
