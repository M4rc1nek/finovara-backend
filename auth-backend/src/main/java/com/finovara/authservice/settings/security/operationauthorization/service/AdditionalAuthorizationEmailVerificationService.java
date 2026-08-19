package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailSender;
import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeRequest;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeResponse;
import com.finovara.authservice.settings.security.operationauthorization.service.attempts.AdditionalAuthorizationAttemptsHandler;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.attempts.VerificationCodeAttemptsTemplate;
import com.finovara.authservice.util.attempts.VerificationCodeVerifier;
import com.finovara.authservice.util.attempts.dto.AttemptsContext;
import com.finovara.authservice.util.attempts.properties.VerificationCodeProperties;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
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
public class AdditionalAuthorizationEmailVerificationService {

    private static final String ATTEMPTS_EXCEEDED_MESSAGE = "Additional authorization code attempts limit exceeded";

    private final VerificationCodeEmailSender verificationCodeEmailSender;
    private final SecretGenerator secretGenerator;
    private final VerificationCodeVerifier verificationCodeVerifier;
    private final VerificationCodeAttemptsTemplate attemptsTemplate;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeProperties properties;
    private final UserManagerService userManagerService;
    private final OutboxService outboxService;

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
    public AdditionalAuthorizationEmailCodeResponse confirmAdditionalAuthorizationCode(Long userId, AdditionalAuthorizationEmailCodeRequest dto, HttpServletRequest httpServletRequest) {
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

        String ipAddress = getClientIpAddress(httpServletRequest);
        outboxService.save("User", userId.toString(), "activity.account-changes",
                new AccountChangesActivityEvent(userId, AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_ENABLED,
                        getBrowser(httpServletRequest), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));

        return new AdditionalAuthorizationEmailCodeResponse(new ConfirmAuthorizationCodeDto(generatedCode), attemptsDto);
    }
}