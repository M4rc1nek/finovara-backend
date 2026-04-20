package com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordResetConfirmDto(
        @Email @NotBlank String email,
        @NotBlank String newPassword,
        @NotBlank String confirmNewPassword,
        @NotNull Integer code
) {
}

