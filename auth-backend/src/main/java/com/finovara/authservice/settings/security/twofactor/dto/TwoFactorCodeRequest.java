package com.finovara.authservice.settings.security.twofactor.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorCodeRequest(@NotBlank String code) {
}
