package com.finovara.authservice.settings.account.service.passwordpolicy.attempts;

import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.repository.AccountRepository;
import com.finovara.authservice.util.attempts.AttemptsHandler;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class PasswordResetAttemptsHandler implements AttemptsHandler {

    private final String email;
    private final AccountRepository accountRepository;

    private AccountSettings accountSettings;

    @Override
    public LocalDateTime getAttemptsExpiresAt() {
        return getAccountSettings().getAttemptsPasswordExpiresAt();
    }

    @Override
    public int getCurrentAttempts() {
        return getAccountSettings().getPasswordResetAttempts();
    }

    @Override
    public int incrementAttempts(int maxAttempts) {
        AccountSettings settings = getAccountSettings();
        if (settings.getPasswordResetAttempts() >= maxAttempts) {
            return 0;
        }
        settings.setPasswordResetAttempts(settings.getPasswordResetAttempts() + 1);
        return 1;
    }

    @Override
    public void resetAttempts(int value, LocalDateTime expiresAt) {
        AccountSettings settings = getAccountSettings();
        settings.setPasswordResetAttempts(value);
        settings.setAttemptsPasswordExpiresAt(expiresAt);
    }

    private AccountSettings getAccountSettings() {
        if (accountSettings == null) {
            accountSettings = accountRepository.findByUserEmailForUpdate(email)
                    .orElseThrow(() -> new RequestedEntityNotFoundException("Account getAccountSettings not found"));
        }
        return accountSettings;
    }
}