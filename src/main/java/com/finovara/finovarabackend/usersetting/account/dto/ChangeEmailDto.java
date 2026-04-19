package com.finovara.finovarabackend.usersetting.account.dto;

import jakarta.validation.constraints.Email;

public record ChangeEmailDto(
        @Email String email,
        String password,
        Integer code
) {
}
