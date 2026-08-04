package com.finovara.authservice.settings.security.operationauthorization.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdditionalAuthorizationEmailCodeRequest(
        @NotNull @Min(100000) @Max(999999) Integer code
) {
}