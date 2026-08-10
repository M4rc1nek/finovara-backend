package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecuritySettingsVisibilityService {

    private final SecuritySettingsRepository securitySettingsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markVisible(SecuritySettings securitySettings) {
        securitySettings.setAdditionalAuthorizationVisible(true);
        securitySettingsRepository.save(securitySettings);
    }
}