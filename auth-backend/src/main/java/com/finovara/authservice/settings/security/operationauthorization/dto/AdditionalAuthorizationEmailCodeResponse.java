package com.finovara.authservice.settings.security.operationauthorization.dto;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;

public record AdditionalAuthorizationEmailCodeResponse(
        ConfirmAuthorizationCodeDto authorizationCode,
        AttemptsDto attempts
) {}