package com.finovara.corebackend.usersetting.account.service;

import com.finovara.contracts.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import com.finovara.corebackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.corebackend.usersetting.notificationemail.action.accountdeleted.service.NotifyOnAccountDeletedService;
import com.finovara.corebackend.usersetting.notificationemail.action.usernamechange.service.NotifyUsernameChangeService;
import com.finovara.contracts.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.corebackend.util.profile.ProfileImageUrlBuilder;
import com.finovara.corebackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotifyUsernameChangeService notifyUsernameChangeService;
    private final NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    @Transactional
    public AccountSettingsDto updateUsername(AccountSettingsDto accountSettingsDto, Long userId, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (userRepository.existsByUsername(accountSettingsDto.username())) {
            throw new EntityAlreadyExistsException("Username is already taken");
        }

        user.setUsername(accountSettingsDto.username());
        userRepository.save(user);
        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("activity.account-changes", new AccountChangesActivityEvent(userId, AccountChangesActivityType.USERNAME_CHANGED, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
        notifyUsernameChangeService.sendEmail(user);
        return accountSettingsDto;
    }

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        passwordValidator.validatePassword(userId, confirmPasswordDto);
        userRepository.delete(user);
        log.info("User account has been deleted. User email: {}", user.getEmail());
        notifyOnAccountDeletedService.sendEmail(user);
    }

    @Transactional
    public AccountSettingsDto getAccountSettings(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        String profileImageUrl = ProfileImageUrlBuilder.buildProfileImageUrl(user.getProfileImagePath());

        return new AccountSettingsDto(user.getUsername(), user.getEmail(), user.getCreatedAt(), profileImageUrl);
    }

}
