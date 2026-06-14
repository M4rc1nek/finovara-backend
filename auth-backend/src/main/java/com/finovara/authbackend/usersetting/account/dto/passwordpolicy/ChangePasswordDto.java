package com.finovara.authbackend.usersetting.account.dto.passwordpolicy;

import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
        @Size(min = 8, max = 55, message = "Hasło musi mieć od 8 do 55 znaków")
        String newPassword,
        String confirmNewPassword
) {
}

