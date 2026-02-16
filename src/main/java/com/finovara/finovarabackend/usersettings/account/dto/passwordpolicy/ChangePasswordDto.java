package com.finovara.finovarabackend.usersettings.account.dto.passwordpolicy;

import jakarta.validation.constraints.Pattern;

public record ChangePasswordDto(
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
                message = "Hasło musi mieć min. 8 znaków, jedną dużą literę, jedną cyfrę i jeden znak specjalny"
        )
        String newPassword,

        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
                message = "Hasło musi mieć min. 8 znaków, jedną dużą literę, jedną cyfrę i jeden znak specjalny"
        )
        String confirmNewPassword
) {
}
