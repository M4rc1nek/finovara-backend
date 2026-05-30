package com.finovara.corebackend.usersetting.account.dto.emailpolicy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeRequestDto(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}

