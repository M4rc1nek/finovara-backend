package com.finovara.finovarabackend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegisterLoginDTO(
        Long id,
        @Size(min = 3, max = 13) String username,


        @NotBlank
        String password,

        @Email
        String email,

        String jwtToken

) {
}
