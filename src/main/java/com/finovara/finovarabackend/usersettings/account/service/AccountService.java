package com.finovara.finovarabackend.usersettings.account.service;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.UserActivityAccountChangesType;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.service.UserActivityAccountChangesService;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersettings.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
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
    private final UserActivityAccountChangesService userActivityAccountChangesService;

    @Transactional
    public AccountSettingsDto updateUsername(AccountSettingsDto accountSettingsDto, Long userId, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (userRepository.existsByUsername(accountSettingsDto.username())) {
            throw new NameAlreadyExistsException("Username is already taken");
        }

        user.setUsername(accountSettingsDto.username());

        userRepository.save(user);
        userActivityAccountChangesService.createUserActivityAccountChanges(user.getEmail(), UserActivityAccountChangesType.USERNAME_CHANGED, request);
        return accountSettingsDto;
    }

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        passwordConfirmationService.confirmPassword(user.getEmail(), confirmPasswordDto);

        userRepository.delete(user);
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