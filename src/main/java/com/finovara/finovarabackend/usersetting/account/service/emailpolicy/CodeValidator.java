package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.usersetting.account.dto.ChangeEmailDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CodeValidator {
    public void verifyCode(AccountSettings settings, ChangeEmailDto dto) {
        Integer code = settings.getEmailChangeCode();

        if (dto.code() == null) {
            throw new InvalidInputException("Code is required");
        }

        if (code == null) {
            throw new InvalidInputException("No code generated");
        }

        if (settings.getEmailChangeCodeExpiresAt() == null ||
                settings.getEmailChangeCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInputException("Code expired");
        }

        if (!code.equals(dto.code())) {
            throw new InvalidInputException("Incorrect code");
        }
    }
}
