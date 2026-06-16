package com.finovara.authservice.settings.factory;

import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsFactory {

    public AccountSettings createDefaultAccountSettings(User user) {
        return AccountSettings.builder()
                .userAssigned(user)
                .emailChangeCode(null)
                .pendingEmail(null)
                .resetPasswordCode(null)
                .resetPasswordCodeExpiresAt(null)
                .attemptsEmailExpiresAt(null)
                .attemptsPasswordExpiresAt(null)
                .build();
    }

}
