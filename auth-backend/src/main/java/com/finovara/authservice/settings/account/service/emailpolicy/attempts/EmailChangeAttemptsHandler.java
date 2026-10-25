package com.finovara.authservice.settings.account.service.emailpolicy.attempts;

import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.repository.AccountRepository;
import com.finovara.authservice.util.attempts.AttemptsHandler;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class EmailChangeAttemptsHandler implements AttemptsHandler {

    private final Long userId;
    private final AccountRepository accountRepository;

    private AccountSettings accountSettings;

    @Override
    public LocalDateTime getAttemptsExpiresAt() {
        return getAccountSettings().getAttemptsEmailExpiresAt();
    }

    @Override
    public int getCurrentAttempts() {
        return getAccountSettings().getEmailChangeAttempts();
    }

    @Override
    public int incrementAttempts(int maxAttempts) {
        AccountSettings settings = getAccountSettings();
        if (settings.getEmailChangeAttempts() >= maxAttempts) {
            return 0;
        }
        settings.setEmailChangeAttempts(settings.getEmailChangeAttempts() + 1);
        return 1;
    }

    @Override
    public void resetAttempts(int value, LocalDateTime expiresAt) {
        AccountSettings settings = getAccountSettings();
        settings.setEmailChangeAttempts(value);
        settings.setAttemptsEmailExpiresAt(expiresAt);
    }

    private AccountSettings getAccountSettings() {
        if (accountSettings == null) {
            accountSettings = accountRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new RequestedEntityNotFoundException("Account getAccountSettings not found"));
        }
        return accountSettings;
    }
}