package com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy;


public record ChangePasswordDto(
         String newPassword,
         String confirmNewPassword
) {
}

