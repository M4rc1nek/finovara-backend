package com.finovara.financeservice.util.sharedaccount.settings;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedSettingsManager {
    private final SharedAccountSettingsRepository sharedAccountSettings;

    public SharedAccountSettings getSharedAccountSettingsByUserId(Long userId) {
        return sharedAccountSettings.findByOwnerIdOrMemberId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Shared account settings not found"));
    }
}

