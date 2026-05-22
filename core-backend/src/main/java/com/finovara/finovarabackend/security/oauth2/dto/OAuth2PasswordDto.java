package com.finovara.finovarabackend.security.oauth2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OAuth2PasswordDto(
        @NotBlank
        @Size(min = 8, max = 55, message = "Hasło musi mieć od 8 do 55 znaków")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
                message = "Hasło musi mieć min. 8 znaków, jedną dużą literę, jedną cyfrę i jeden znak specjalny"
        )
        String password
) {
}
