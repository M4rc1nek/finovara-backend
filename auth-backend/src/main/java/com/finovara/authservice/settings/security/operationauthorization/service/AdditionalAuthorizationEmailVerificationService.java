package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailSender;
import com.finovara.authservice.util.attempts.properties.VerificationCodeProperties;
import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeRequest;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeResponse;
import com.finovara.authservice.settings.security.operationauthorization.service.attempts.AdditionalAuthorizationAttemptsHandler;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.attempts.dto.AttemptsContext;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdditionalAuthorizationEmailVerificationService {

    private static final String ATTEMPTS_EXCEEDED_MESSAGE = "Additional authorization code attempts limit exceeded";

    private final VerificationCodeEmailSender verificationCodeEmailSender;
    private final SecretGenerator secretGenerator;
    private final com.finovara.authservice.util.attempts.VerificationCodeVerifier verificationCodeVerifier;
    private final com.finovara.authservice.util.attempts.VerificationCodeAttemptsTemplate attemptsTemplate;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeProperties properties;
    private final UserManagerService userManagerService;

    @Transactional
    public void requestAdditionalAuthorizationEmail(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        SecuritySettings settings = user.getSecuritySettings();

        int code = secretGenerator.generateSecureCode();
        settings.setAdditionalAuthorizationEmailCode(code);
        settings.setAdditionalAuthorizationEmailCodeExpiresAt(LocalDateTime.now().plusMinutes(properties.getCodeExpirationMinutes()));
        securitySettingsRepository.save(settings);

        verificationCodeEmailSender.sendAuthorizationConfirmCode(user, user.getEmail(), code);
    }

    @Transactional
    public AdditionalAuthorizationEmailCodeResponse confirmAdditionalAuthorizationCode(Long userId, AdditionalAuthorizationEmailCodeRequest dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        SecuritySettings securitySettings = user.getSecuritySettings();

        AttemptsContext context = new AttemptsContext(properties.getMaxAttempts(), properties.getAttemptsExpirationMinutes(), ATTEMPTS_EXCEEDED_MESSAGE);

        verificationCodeVerifier.verifyAttemptsOrThrow(
                securitySettings.getAdditionalAuthorizationEmailCode(),
                securitySettings.getAdditionalAuthorizationEmailCodeExpiresAt(),
                dto.code(),
                context,
                new AdditionalAuthorizationAttemptsHandler(userId, securitySettings, securitySettingsRepository)
        );

        AttemptsDto attemptsDto = attemptsTemplate.getCurrent(context, securitySettingsRepository.getAdditionalAuthorizationAttemptsByUserId(userId));

        String generatedCode = secretGenerator.generateAdditionalAuthorizationCode();
        securitySettings.setAdditionalAuthorizationCode(passwordEncoder.encode(generatedCode));
        securitySettings.setAdditionalAuthorizationEnabled(true);

        securitySettings.setAdditionalAuthorizationEmailCode(null);
        securitySettings.setAdditionalAuthorizationEmailCodeExpiresAt(null);
        securitySettings.setAdditionalAuthorizationAttempts(0);
        securitySettingsRepository.save(securitySettings);

        return new AdditionalAuthorizationEmailCodeResponse(new ConfirmAuthorizationCodeDto(generatedCode), attemptsDto);
    }
}