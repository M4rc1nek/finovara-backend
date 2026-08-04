package com.finovara.authservice.settings.security.operationauthorization.dto;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;

public record ConfirmAuthorizationEmailCodeResultDto(
        ConfirmAuthorizationCodeDto authorizationCode,
        AttemptsDto attempts
) {}