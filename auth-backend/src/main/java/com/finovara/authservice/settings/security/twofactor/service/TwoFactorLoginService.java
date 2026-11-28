package com.finovara.authservice.twofactor.service;

import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.security.jwt.AccessTokenCookie;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.twofactor.cookie.TokenDelivery;
import com.finovara.authservice.twofactor.cookie.TwoFactorPendingLoginCookie;
import com.finovara.authservice.twofactor.dto.TwoFactorLoginResponseDto;
import com.finovara.authservice.user.dto.UserLoginDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.profile.ProfileImageUrlBuilder;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.finovara.contracts.user.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.user.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.user.clientdata.location.UserLocation.getLocationFromIp;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorLoginService {

    private final UserRepository userRepository;
    private final UserManagerService userManagerService;
    private final TwoFactorSettingsService twoFactorSettingsService;
    private final TwoFactorPendingLoginCookie pendingLoginCookie;
    private final RiskGuardService riskGuardService;
    private final JwtService jwtService;
    private final OutboxService outboxService;

    @Transactional
    public UserLoginDto startPasswordLogin(Long userId, String sourceEventId, HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findWithSecuritySettingsByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Incorrect email or password"));

        if (isTwoFactorEnabled(user)) {
            pendingLoginCookie.add(response, user.getId(), sourceEventId,
                    TokenDelivery.RESPONSE, request.isSecure());
            log.info("Two-factor login challenge started. userId={}", user.getId());
            return new UserLoginDto(user.getId(), user.getUsername(), user.getEmail(), null,
                    user.getProfileImageUrl(), null, sourceEventId, true);
        }

        pendingLoginCookie.clear(response);
        riskGuardService.guard(user.getId(), RiskTriggerType.LOGIN, user.getEmail(), sourceEventId, request);
        publishLoginActivity(user.getId(), LoginActivityStatus.SUCCESSFUL, request);
        String jwtToken = jwtService.generateToken(user);
        log.info("Password login completed. userId={}", user.getId());

        return new UserLoginDto(user.getId(), user.getUsername(), user.getEmail(), null,
                user.getProfileImageUrl(), jwtToken, sourceEventId, false);
    }

    public void startLoginChallenge(
            Long userId,
            String sourceEventId,
            TokenDelivery tokenDelivery,
            HttpServletResponse response,
            boolean secure) {
        pendingLoginCookie.add(response, userId, sourceEventId, tokenDelivery, secure);
    }

    public TwoFactorLoginResponseDto verifyLoginCode(String code, HttpServletRequest request, HttpServletResponse response) {
        TwoFactorPendingLoginCookie.PendingLogin pending = pendingLoginCookie.read(request)
                .orElseThrow(() -> new InvalidCredentialsException("Two-factor login session expired"));
        User user = userManagerService.getUserWithSecuritySettingsByIdOrThrow(pending.userId());

        riskGuardService.guard(user.getId(), RiskTriggerType.LOGIN, user.getEmail(), pending.sourceEventId(), request);
        if (!twoFactorSettingsService.verifyLoginChallengeCode(user.getId(), code)) {
            log.warn("Two-factor login verification failed. userId={}", user.getId());
            publishLoginActivity(user.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new InvalidCredentialsException("Invalid authenticator code");
        }

        publishLoginActivity(user.getId(), LoginActivityStatus.SUCCESSFUL, request);
        log.info("Two-factor login completed. userId={}", user.getId());
        String jwtToken = jwtService.generateToken(user);
        if (pending.tokenDelivery() == TokenDelivery.COOKIE) {
            AccessTokenCookie.add(response, jwtToken, request.isSecure());
            jwtToken = null;
        }
        pendingLoginCookie.clear(response);

        return new TwoFactorLoginResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                ProfileImageUrlBuilder.buildProfileImageUrl(user.getProfileImagePath()),
                user.isPasswordSet(),
                jwtToken);
    }

    public void cancelLogin(HttpServletResponse response) {
        pendingLoginCookie.clear(response);
    }

    private boolean isTwoFactorEnabled(User user) {
        return user.getSecuritySettings() != null && user.getSecuritySettings().isTwoFactorEnabled();
    }

    private void publishLoginActivity(Long userId, LoginActivityStatus status, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        outboxService.save("User", userId.toString(), "user.logged-in",
                new LoginActivityEvent(userId, status, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}
