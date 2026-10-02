package com.finovara.authservice.settings.security.operationauthorization.dto;

import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AdditionalAuthorizationRequest(
        @NotNull boolean additionalAuthorizationEnabled,
        @Valid @NotNull ConfirmPasswordDto confirmPasswordDto
) {
}