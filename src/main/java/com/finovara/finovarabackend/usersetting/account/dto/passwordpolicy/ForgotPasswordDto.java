package com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordDto(
        @Email @NotBlank String email,
        Integer code
) {
}
