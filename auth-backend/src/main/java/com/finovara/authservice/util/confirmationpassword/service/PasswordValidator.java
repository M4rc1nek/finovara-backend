package com.finovara.authservice.util.confirmationpassword.service;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.exception.forbidden.InvalidPasswordException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordValidator {

    private final UserManagerService userManagerService;
    private final PasswordEncoder passwordEncoder;

    public void validatePassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (!passwordEncoder.matches(confirmPasswordDto.password(), user.getPassword())) {
            throw new InvalidPasswordException("Incorrect password");
        }

    }

}
