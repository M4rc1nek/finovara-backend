package com.finovara.finovarabackend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegisterLoginDTO(
        Long id,
        String username,
        @NotBlank
        String password,

        @Email
        String email,

        String jwtToken

) {
}
