package com.finovara.authservice.settings.account.service;

import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.event.user.UserAccountDeletedEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.settings.account.dto.AccountSettingsDto;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.profile.ProfileImageUrlBuilder;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.outbox.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final UserManagerService userManagerService;
    private final PasswordValidator passwordValidator;
    private final OutboxService outboxService;

    @Transactional
    public AccountSettingsDto updateUsername(AccountSettingsDto accountSettingsDto, Long userId, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (userRepository.existsByUsername(accountSettingsDto.username())) {
            throw new EntityAlreadyExistsException("Username is already taken");
        }

        user.setUsername(accountSettingsDto.username());
        userRepository.save(user);

        String ipAddress = getClientIpAddress(request);
        outboxService.save("User", userId.toString(), "activity.account-changes",
                new AccountChangesActivityEvent(userId, AccountChangesActivityType.USERNAME_CHANGED, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
        outboxService.save("User", userId.toString(), "notification.email.send",
                new SendEmailEvent(user.getId(), user.getUsername(), user.getEmail(), "Finovara - Zmiana nazwy użytkownika", "email/username-changed.html"));

        return accountSettingsDto;
    }

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        passwordValidator.validatePassword(userId, confirmPasswordDto);

        outboxService.save("User", userId.toString(), "notification.email.send",
                new SendEmailEvent(user.getId(), user.getUsername(), user.getEmail(), "Finovara - Usuniecie konta", "email/account-deleted.html"));
        outboxService.save("User", userId.toString(), "user-account.deleted",
                new UserAccountDeletedEvent(user.getId()));

        userRepository.delete(user);
        log.info("User account has been deleted. User email: {}", user.getEmail());
    }

    @Transactional
    public AccountSettingsDto getAccountSettings(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        String profileImageUrl = ProfileImageUrlBuilder.buildProfileImageUrl(user.getProfileImagePath());

        return new AccountSettingsDto(user.getUsername(), user.getEmail(), user.getCreatedAt(), profileImageUrl);
    }
}