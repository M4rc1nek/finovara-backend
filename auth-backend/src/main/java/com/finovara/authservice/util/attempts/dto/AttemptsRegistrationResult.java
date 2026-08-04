package com.finovara.authservice.util.attempts.dto;

import com.finovara.authservice.settings.account.dto.AttemptsDto;

public record AttemptsRegistrationResult(
        AttemptsDto attempts,
        boolean limitExceeded
) {
}