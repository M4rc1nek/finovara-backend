package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationDto;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationRequest;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.exception.forbidden.InvalidPasswordException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdditionalAuthorizationService {

    private final SecuritySettingsRepository securitySettingsRepository;
    private final SecretGenerator secretGenerator;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    @Transactional
    public ConfirmAuthorizationCodeDto saveAdditionalAuthorization(Long userId, AdditionalAuthorizationRequest request) {
        passwordValidator.validatePassword(userId, request.confirmPasswordDto());

        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);
        securitySettings.setAdditionalAuthorizationEnabled(request.additionalAuthorizationEnabled());

        if (!request.additionalAuthorizationEnabled()) {
            securitySettings.setAdditionalAuthorizationCode(null);
            return null;
        }

        String generatedCode = secretGenerator.generateAdditionalAuthorizationCode();
        securitySettings.setAdditionalAuthorizationCode(passwordEncoder.encode(generatedCode));

        return new ConfirmAuthorizationCodeDto(generatedCode);
    }

    public AdditionalAuthorizationDto getAdditionalAuthorizationSettings(Long userId) {
        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);
        return new AdditionalAuthorizationDto(securitySettings.isAdditionalAuthorizationEnabled());
    }

    @Transactional
    public ConfirmAuthorizationCodeDto regenerateCode(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        passwordValidator.validatePassword(userId, confirmPasswordDto);

        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);

        if (!securitySettings.isAdditionalAuthorizationEnabled()) {
            throw new InvalidPasswordException("Additional authorization is not enabled");
        }

        String generatedCode = secretGenerator.generateAdditionalAuthorizationCode();
        securitySettings.setAdditionalAuthorizationCode(passwordEncoder.encode(generatedCode));

        return new ConfirmAuthorizationCodeDto(generatedCode);
    }

    public void confirmAdditionalAuthorizationCode(Long userId, ConfirmAuthorizationCodeDto confirmAuthorizationCodeDto) {
        SecuritySettings securitySettings = securitySettingsRepository.findByUserId(userId);

        if (!securitySettings.isAdditionalAuthorizationEnabled()) {
            return;
        }

        if (confirmAuthorizationCodeDto == null || confirmAuthorizationCodeDto.code() == null) {
            throw new InvalidPasswordException("Authorization code is required");
        }

        if (!passwordEncoder.matches(confirmAuthorizationCodeDto.code(), securitySettings.getAdditionalAuthorizationCode())) {
            throw new InvalidPasswordException("Incorrect authorization code");
        }
    }
}