package com.finovara.authservice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequestDto(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,

        String riskVerificationSourceEventId
) {
}
