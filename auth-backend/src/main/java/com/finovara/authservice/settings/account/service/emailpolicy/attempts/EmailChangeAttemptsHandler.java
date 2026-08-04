package com.finovara.authservice.settings.account.service.emailpolicy;

import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.repository.AccountRepository;
import com.finovara.authservice.util.attempts.AttemptsHandler;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class EmailChangeAttemptsHandler implements AttemptsHandler {

    private final Long userId;
    private final AccountSettings settings;
    private final AccountRepository accountRepository;


    @Override
    public LocalDateTime getAttemptsExpiresAt() {
        return settings.getAttemptsEmailExpiresAt();
    }

    @Override
    public int getCurrentAttempts() {
        return accountRepository.getEmailChangeAttemptsByUserId(userId);
    }

    @Override
    public int incrementAttempts(int maxAttempts) {
        return accountRepository.incrementEmailChangeAttempts(userId, maxAttempts);
    }

    @Override
    public void resetAttempts(int value, LocalDateTime expiresAt) {
        settings.setEmailChangeAttempts(value);
        settings.setAttemptsEmailExpiresAt(expiresAt);
        accountRepository.save(settings);
    }
}