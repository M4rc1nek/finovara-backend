package com.finovara.authservice.security.oauth2;

import static com.finovara.contracts.user.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.user.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.user.clientdata.location.UserLocation.getLocationFromIp;

import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.security.jwt.AccessTokenCookie;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.security.oauth2.dto.OAuth2LoginResponseDto;
import com.finovara.authservice.settings.security.twofactor.service.TwoFactorLoginService;
import com.finovara.authservice.settings.security.twofactor.cookie.TokenDelivery;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.profile.ProfileImageUrlBuilder;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OAuth2LoginCompletionService {

    private final OAuth2PendingLoginCookie pendingLoginCookie;
    private final TwoFactorLoginService twoFactorLoginService;
    private final UserManagerService userManagerService;
    private final RiskGuardService riskGuardService;
    private final JwtService jwtService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OAuth2LoginResponseDto complete(HttpServletRequest request, HttpServletResponse response) {
        OAuth2PendingLoginCookie.PendingLogin pending = pendingLoginCookie.readPendingLogin(request)
                .orElseThrow(() -> new InvalidCredentialsException("OAuth2 login session expired"));

        User user = userManagerService.getUserWithSecuritySettingsByIdOrThrow(pending.userId());

        if (user.getSecuritySettings() != null && user.getSecuritySettings().isTwoFactorEnabled()) {
            twoFactorLoginService.startLoginChallenge(user.getId(), pending.sourceEventId(), TokenDelivery.COOKIE, response, request.isSecure());
            pendingLoginCookie.clearPendingLogin(response);
            return new OAuth2LoginResponseDto(user.getId(), user.getUsername(), user.getEmail(), ProfileImageUrlBuilder.buildProfileImageUrl(user.getProfileImagePath()), user.isPasswordSet(), true);
        }

        riskGuardService.guard(user.getId(), RiskTriggerType.LOGIN, user.getEmail(), pending.sourceEventId(), request);

        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("user.logged-in", new LoginActivityEvent(user.getId(), LoginActivityStatus.SUCCESSFUL, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));

        AccessTokenCookie.add(response, jwtService.generateToken(user), request.isSecure());
        pendingLoginCookie.clearPendingLogin(response);

        return new OAuth2LoginResponseDto(user.getId(), user.getUsername(), user.getEmail(), ProfileImageUrlBuilder.buildProfileImageUrl(user.getProfileImagePath()), user.isPasswordSet(), false);
    }
}