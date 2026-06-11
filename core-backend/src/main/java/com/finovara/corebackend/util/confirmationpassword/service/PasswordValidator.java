package com.finovara.corebackend.util.confirmationpassword.service;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.exception.unauthorized.InvalidCredentialsException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.util.user.service.UserManagerService;
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
            throw new InvalidCredentialsException("Incorrect password");
        }

    }

}
