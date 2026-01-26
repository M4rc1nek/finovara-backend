package com.finovara.finovarabackend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterLoginDTO(
        Long id,
        @Size(min = 3, max = 13)
        String username,

        @NotBlank
        @Size(min = 8)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
                message = "Hasło musi mieć min. 8 znaków, jedną dużą literę, jedną cyfrę i jeden znak specjalny"
        )
        String password,

        @Email
        @NotBlank
        String email,

        String jwtToken

) {
}
