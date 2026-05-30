package com.finovara.corebackend.usersetting.account.dto.emailpolicy;

import jakarta.validation.constraints.NotNull;

public record EmailChangeConfirmDto(
        @NotNull Integer code
) {
}

