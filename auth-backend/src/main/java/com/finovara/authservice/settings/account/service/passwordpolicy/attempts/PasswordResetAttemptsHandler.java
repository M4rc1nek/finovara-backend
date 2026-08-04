package com.finovara.authservice.settings.account.service.passwordpolicy;

import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.repository.AccountRepository;
import com.finovara.authservice.util.attempts.AttemptsHandler;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class PasswordResetAttemptsHandler implements AttemptsHandler {

    private final String email;
    private final AccountSettings settings;
    private final AccountRepository accountRepository;

    @Override
    public LocalDateTime getAttemptsExpiresAt() {
        return settings.getAttemptsPasswordExpiresAt();
    }

    @Override
    public int getCurrentAttempts() {
        return accountRepository.getPasswordResetAttemptsByUserEmail(email);
    }

    @Override
    public int incrementAttempts(int maxAttempts) {
        return accountRepository.incrementPasswordResetAttempts(email, maxAttempts);
    }

    @Override
    public void resetAttempts(int value, LocalDateTime expiresAt) {
        settings.setPasswordResetAttempts(value);
        settings.setAttemptsPasswordExpiresAt(expiresAt);
        accountRepository.save(settings);
    }
}