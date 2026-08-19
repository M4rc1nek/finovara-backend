package com.finovara.authservice.settings.account.service;

import com.finovara.authservice.settings.account.dto.AccountSettingsDto;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.notification.email.ActionEmailEventType;
import com.finovara.contracts.notification.event.SendEmailEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final UserManagerService userManagerService;
    private final OutboxService outboxService;
    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public AccountSettingsDto updateUsername(AccountSettingsDto accountSettingsDto, Long userId, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(accountSettingsDto.authorizationCode()));

        if (userRepository.existsByUsername(accountSettingsDto.username())) {
            throw new EntityAlreadyExistsException("Username is already taken");
        }

        user.setUsername(accountSettingsDto.username());
        userRepository.save(user);

        String ipAddress = getClientIpAddress(request);
        outboxService.save("User", userId.toString(), "activity.account-changes",
                new AccountChangesActivityEvent(userId, AccountChangesActivityType.USERNAME_CHANGED, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
        outboxService.save("User", userId.toString(), "notification.email.send",
                new SendEmailEvent(user.getId(), user.getUsername(), user.getEmail(),
                        ActionEmailEventType.USERNAME_CHANGED, Map.of()));

        return accountSettingsDto;
    }

    @Transactional
    public AccountSettingsDto getAccountSettings(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        String profileImageUrl = user.getProfileImageUrl();

        return new AccountSettingsDto(user.getUsername(), user.getEmail(), user.getCreatedAt(), profileImageUrl, null);
    }
}