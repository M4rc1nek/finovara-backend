package com.finovara.authservice.settings.account.service.profileimage;

import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity.AccountChangeActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.LocalDateTime;

import static com.finovara.contracts.user.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.user.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.user.clientdata.location.UserLocation.getLocationFromIp;

@Service
@RequiredArgsConstructor
public class ProfileImageUpdateService {

    private final UserManagerService userManagerService;
    private final OutboxService outboxService;
    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    private final RiskGuardService riskGuardService;

    @Value("${application.upload.profile-images-default-directory}")
    private String profileImagesDefaultDirectory;

    @Transactional
    public String replaceProfileImage(Long userId, String newImagePath, HttpServletRequest request, String authorizationCode, String sourceEventId) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        User user = userManagerService.getUserByIdOrThrow(userId);
        riskGuardService.guard(userId, RiskTriggerType.PROFILE_IMAGE_CHANGED, user.getEmail(), sourceEventId, request);

        String previousImagePath = user.getProfileImagePath();
        user.setProfileImagePath(newImagePath);

        publishActivity(userId, AccountChangeActivityType.PROFILE_IMG_CHANGED, request);
        return previousImagePath;
    }

    @Transactional
    public String resetProfileImage(Long userId, HttpServletRequest request, String authorizationCode, String sourceEventId) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        User user = userManagerService.getUserByIdOrThrow(userId);
        String currentImagePath = user.getProfileImagePath();

        if (currentImagePath == null || currentImagePath.contains("UserProf.png")) {
            throw new IllegalArgumentException("Profile image does not exist or is already default");
        }

        riskGuardService.guard(userId, RiskTriggerType.PROFILE_IMAGE_CHANGED, user.getEmail(), sourceEventId, request);

        user.setProfileImagePath(Paths.get(profileImagesDefaultDirectory).resolve("UserProf.png").toString());

        publishActivity(userId, AccountChangeActivityType.PROFILE_IMG_DELETED, request);
        return currentImagePath;
    }

    private void publishActivity(Long userId, AccountChangeActivityType type, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        outboxService.save("User", userId.toString(), "account.changed",
                new AccountChangeActivityEvent(userId, type, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}