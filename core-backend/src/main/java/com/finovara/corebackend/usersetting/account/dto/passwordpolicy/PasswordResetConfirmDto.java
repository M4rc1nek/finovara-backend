package com.finovara.corebackend.usersetting.account.dto.passwordpolicy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmDto(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 55, message = "Hasło musi mieć od 8 do 55 znaków")
        String newPassword,
        @NotBlank String confirmNewPassword,
        @NotNull Integer code
) {
}

