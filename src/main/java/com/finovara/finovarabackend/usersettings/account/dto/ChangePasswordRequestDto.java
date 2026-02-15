package com.finovara.finovarabackend.usersettings.account.dto;

import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;

public record ChangePasswordRequestDto(
        ConfirmPasswordDto confirmPasswordDto,
        @Valid ChangePasswordDto changePasswordDto
) {
}
