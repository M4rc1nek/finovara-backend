package com.finovara.finovarabackend.usersetting.account.service;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.service.user.accountmanagment.accountpolicy.accountdeleted.AccountDeletedEmailService;
import com.finovara.finovarabackend.util.service.user.accountmanagment.usernamepolicy.UsernameChangeEmailService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final UserManagerService userManagerService;
    private final PasswordConfirmationService passwordConfirmationService;
    private final AccountChangesActivityService accountChangesActivityService;
    private final UsernameChangeEmailService usernameChangeEmailService;
    private final AccountDeletedEmailService accountDeletedEmailService;

    @Transactional
    public AccountSettingsDto updateUsername(AccountSettingsDto accountSettingsDto, Long userId, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        NotificationSettings settings = user.getNotificationSettings();

        if (userRepository.existsByUsername(accountSettingsDto.username())) {
            throw new NameAlreadyExistsException("Username is already taken");
        }

        user.setUsername(accountSettingsDto.username());
        userRepository.save(user);
        accountChangesActivityService.createAccountChangesActivity(user.getEmail(), AccountChangesActivityType.USERNAME_CHANGED, request);
        if(settings.isNotifyOnUsernameChange()){
            usernameChangeEmailService.sendEmail(user);
        }
        return accountSettingsDto;
    }

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        NotificationSettings settings = user.getNotificationSettings();

        passwordConfirmationService.confirmPassword(user.getEmail(), confirmPasswordDto);
        userRepository.delete(user);
        if(settings.isNotifyOnAccountDeleted()){
            accountDeletedEmailService.sendEmail(user);
        }
    }

    @Transactional
    public AccountSettingsDto getAccountSettings(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        String profileImageUrl = buildProfileImageUrl(user.getProfileImagePath());

        return new AccountSettingsDto(user.getUsername(), user.getEmail(), user.getCreatedAt(), profileImageUrl);
    }

    private String buildProfileImageUrl(String profileImagePath) {
        if (profileImagePath == null) {
            return null;
        }
        String filename = Paths.get(profileImagePath).getFileName().toString();
        return "/profile-images/" + filename;
    }

}