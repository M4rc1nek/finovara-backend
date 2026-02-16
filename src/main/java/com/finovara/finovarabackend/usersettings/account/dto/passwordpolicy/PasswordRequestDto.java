package com.finovara.finovarabackend.usersettings.account.dto.passwordpolicy;

import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;

public record PasswordRequestDto(
        ConfirmPasswordDto confirmPasswordDto,
        @Valid ChangePasswordDto changePasswordDto,
        @Valid ForgotPasswordDto forgotPasswordDto
) {
}
