package com.finovara.authservice.security.oauth2;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;

import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.security.oauth2.OAuth2PendingLoginCookie.PendingLogin;
import com.finovara.authservice.security.oauth2.dto.OAuth2LoginResponseDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.profile.ProfileImageUrlBuilder;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
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
    private final UserManagerService userManagerService;
    private final RiskGuardService riskGuardService;
    private final JwtService jwtService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OAuth2LoginResponseDto complete(HttpServletRequest request, HttpServletResponse response) {
        PendingLogin pending = pendingLoginCookie.read(request)
                .orElseThrow(() -> new InvalidCredentialsException("OAuth2 login session expired"));

        User user = userManagerService.getUserByIdOrThrow(pending.userId());

        riskGuardService.guard(user.getId(), RiskTriggerType.LOGIN, user.getEmail(), pending.sourceEventId(), request);

        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("user.logged-in", new LoginActivityEvent(user.getId(), LoginActivityStatus.SUCCESSFUL, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));

        OAuth2AccessTokenCookie.add(response, jwtService.generateToken(user), request.isSecure());
        pendingLoginCookie.clear(response);

        return new OAuth2LoginResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                ProfileImageUrlBuilder.buildProfileImageUrl(user.getProfileImagePath()),
                user.isPasswordSet());
    }
}