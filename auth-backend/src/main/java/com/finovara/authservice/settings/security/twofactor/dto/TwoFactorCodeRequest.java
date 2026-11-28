package com.finovara.authservice.twofactor.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorCodeRequest(@NotBlank String code) {
}
