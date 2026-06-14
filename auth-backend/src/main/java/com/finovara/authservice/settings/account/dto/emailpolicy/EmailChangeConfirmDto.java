package com.finovara.authservice.settings.account.dto.emailpolicy;

import jakarta.validation.constraints.NotNull;

public record EmailChangeConfirmDto(
        @NotNull Integer code
) {
}

