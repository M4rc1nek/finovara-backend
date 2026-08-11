package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationSettingsResponse;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationRequest;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.exception.forbidden.InvalidPasswordException;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;

@Service
@RequiredArgsConstructor
public class AdditionalAuthorizationService {

    private final PasswordEncoder passwordEncoder;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final PasswordValidator passwordValidator;
    private final OutboxService outboxService;
    private final SecuritySettingsVisibilityService securitySettingsVisibilityService;

    @Transactional
    public void saveAdditionalAuthorization(Long userId, AdditionalAuthorizationRequest additionalAuthorizationRequest, HttpServletRequest httpServletRequest) {
        passwordValidator.validatePassword(userId, additionalAuthorizationRequest.confirmPasswordDto());

        if (additionalAuthorizationRequest.additionalAuthorizationEnabled()) {
            return;
        }

        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);
        securitySettings.setAdditionalAuthorizationEnabled(false);
        securitySettings.setAdditionalAuthorizationCode(null);
        securitySettingsRepository.save(securitySettings);

        String ipAddress = getClientIpAddress(httpServletRequest);
        outboxService.save("User", userId.toString(), "activity.account-changes",
                new AccountChangesActivityEvent(userId, AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_DISABLED,
                        getBrowser(httpServletRequest), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }

    public AdditionalAuthorizationSettingsResponse getAdditionalAuthorizationSettings(Long userId) {
        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);
        return new AdditionalAuthorizationSettingsResponse(securitySettings.isAdditionalAuthorizationEnabled());
    }

    @Transactional
    public void regenerateCode(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        passwordValidator.validatePassword(userId, confirmPasswordDto);

        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);
        if (!securitySettings.isAdditionalAuthorizationEnabled()) {
            throw new InvalidPasswordException("Additional authorization is not enabled");
        }
    }

    public Boolean visibleAdditionalAuthorization(Long userId){
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId);
        return settings.isAdditionalAuthorizationVisible();
    }

    @Transactional
    public void confirmAdditionalAuthorizationCode(Long userId, ConfirmAuthorizationCodeDto confirmAuthorizationCodeDto) {
        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);

        if (!securitySettings.isAdditionalAuthorizationEnabled()) {
            return;
        }

        if (confirmAuthorizationCodeDto == null || confirmAuthorizationCodeDto.code() == null) {
            securitySettingsVisibilityService.markVisible(securitySettings);
            throw new InvalidPasswordException("Authorization code is required");
        }

        if (!passwordEncoder.matches(confirmAuthorizationCodeDto.code(), securitySettings.getAdditionalAuthorizationCode())) {
            securitySettingsVisibilityService.markVisible(securitySettings);
            throw new InvalidPasswordException("Incorrect authorization code");
        }

        securitySettings.setAdditionalAuthorizationVisible(false);
        securitySettingsRepository.save(securitySettings);
    }
}