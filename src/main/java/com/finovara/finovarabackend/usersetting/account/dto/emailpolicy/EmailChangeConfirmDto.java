package com.finovara.finovarabackend.usersetting.account.dto.emailpolicy;

import jakarta.validation.constraints.NotNull;

public record EmailChangeConfirmDto(
        @NotNull Integer code
) {
}

