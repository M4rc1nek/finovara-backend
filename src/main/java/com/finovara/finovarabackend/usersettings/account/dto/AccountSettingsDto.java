package com.finovara.finovarabackend.usersettings.account.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AccountSettingsDto(
        @Size(min = 3, max = 13)
        String username,

        String email,
        LocalDateTime createdAt,
        String profileImageUrl

) {
}

// i zdjecie profilowe.. zdjecie zaraz bedzie jak cos.
//Dam caly kod a ty dostosuj caly frontend pod to co wyslalem, utworz wszelkie walidacje i czerwone komunikaty jezeli cos pojdzie nie tak.
//Tu masz caly kod: