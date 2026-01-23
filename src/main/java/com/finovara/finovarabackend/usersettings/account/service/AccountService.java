package com.finovara.finovarabackend.usersettings.account.service;

import com.finovara.finovarabackend.exception.NameAlreadyExistsException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.exception.WrongPasswordException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersettings.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersettings.account.dto.ConfirmPasswordDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AccountSettingsDto updateUsername(AccountSettingsDto accountSettingsDto, Long userId) {
        User user = getUserByIdOrThrow(userId);

        if (userRepository.existsByUsername(accountSettingsDto.username())) {
            throw new NameAlreadyExistsException("Username is already taken");
        }

        user.setUsername(accountSettingsDto.username());

        userRepository.save(user);
        return accountSettingsDto;
    }

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        User user = getUserByIdOrThrow(userId);

        if (!passwordEncoder.matches(confirmPasswordDto.password(), user.getPassword())) {
            throw new WrongPasswordException("You cannot delete your account, incorrect password");
        }

        userRepository.delete(user);
    }

    @Transactional
    public AccountSettingsDto getAccountSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

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

    private User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}